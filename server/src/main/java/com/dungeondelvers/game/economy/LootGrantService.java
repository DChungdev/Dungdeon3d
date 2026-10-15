package com.dungeondelvers.game.economy;

import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.entity.CurrencyAuditLogEntity;
import com.dungeondelvers.persistence.entity.InventoryItemEntity;
import com.dungeondelvers.persistence.entity.ItemAuditLogEntity;
import com.dungeondelvers.persistence.entity.ItemDefinitionEntity;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import com.dungeondelvers.persistence.repository.CurrencyAuditLogRepository;
import com.dungeondelvers.persistence.repository.InventoryItemRepository;
import com.dungeondelvers.persistence.repository.ItemAuditLogRepository;
import com.dungeondelvers.persistence.repository.MobKillRewardRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pays out one mob kill to one character in a single transaction: idempotency marker, gold +10
 * with audit, one loot roll into the bag (or rejected if full).
 */
@Service
public class LootGrantService {

    /** Phase 0 checklist: gold +10 per mob. */
    public static final int GOLD_PER_KILL = 10;

    private static final Logger log = LoggerFactory.getLogger(LootGrantService.class);

    /** An item that landed in the bag; {@code quantity} is the amount added by this drop. */
    public record LootItem(UUID inventoryItemId, String itemDefId, String name, String rarity, int quantity) {}

    public record GrantResult(List<LootItem> items, int gold, int goldTotal, boolean inventoryFull) {}

    private final MobKillRewardRepository rewardRepository;
    private final CharacterRepository characterRepository;
    private final InventoryItemRepository inventoryRepository;
    private final CurrencyAuditLogRepository currencyAuditRepository;
    private final ItemAuditLogRepository itemAuditRepository;
    private final ItemCatalog itemCatalog;

    public LootGrantService(
            MobKillRewardRepository rewardRepository,
            CharacterRepository characterRepository,
            InventoryItemRepository inventoryRepository,
            CurrencyAuditLogRepository currencyAuditRepository,
            ItemAuditLogRepository itemAuditRepository,
            ItemCatalog itemCatalog
    ) {
        this.rewardRepository = rewardRepository;
        this.characterRepository = characterRepository;
        this.inventoryRepository = inventoryRepository;
        this.currencyAuditRepository = currencyAuditRepository;
        this.itemAuditRepository = itemAuditRepository;
        this.itemCatalog = itemCatalog;
    }

    /**
     * Returns empty if this character was already rewarded for this kill (or no longer exists),
     * so retries and duplicate kill events never pay twice.
     */
    @Transactional
    public Optional<GrantResult> grant(UUID mobEntityId, String mobDefId, String lootTableId, UUID characterId) {
        if (rewardRepository.insertIfAbsent(mobEntityId, characterId, mobDefId) == 0) {
            log.warn("Duplicate reward for mob {} / character {} ignored", mobEntityId, characterId);
            return Optional.empty();
        }
        // Row lock: concurrent kills for the same character are applied one after another.
        CharacterEntity character = characterRepository.lockById(characterId).orElse(null);
        if (character == null) {
            return Optional.empty();
        }

        character.setGold(character.getGold() + GOLD_PER_KILL);
        currencyAuditRepository.save(new CurrencyAuditLogEntity(
                characterId, "GOLD", GOLD_PER_KILL, character.getGold(), "MOB_KILL", mobEntityId));

        List<LootItem> items = new ArrayList<>();
        boolean inventoryFull = false;
        Optional<LootRoller.Drop> drop = LootRoller.roll(itemCatalog.lootTable(lootTableId), ThreadLocalRandom.current());
        if (drop.isPresent()) {
            Optional<ItemDefinitionEntity> def = itemCatalog.item(drop.get().itemDefId());
            if (def.isEmpty()) {
                log.error("Loot table {} references unknown item {}", lootTableId, drop.get().itemDefId());
            } else if (!addToBag(characterId, def.get(), drop.get(), mobEntityId, items)) {
                inventoryFull = true;
            }
        }
        return Optional.of(new GrantResult(items, GOLD_PER_KILL, character.getGold(), inventoryFull));
    }

    private boolean addToBag(UUID characterId, ItemDefinitionEntity def, LootRoller.Drop drop, UUID mobEntityId,
                             List<LootItem> out) {
        List<InventoryItemEntity> bag = inventoryRepository.findByCharacterIdOrderBySlotIndexAsc(characterId);
        List<InventoryPlanner.Slot> slots = bag.stream()
                .map(i -> new InventoryPlanner.Slot(i.getId(), i.getSlotIndex(), i.getItemDefId(), i.getRarity(),
                        i.getQuantity()))
                .toList();
        Optional<InventoryPlanner.Plan> plan = InventoryPlanner.plan(slots,
                new InventoryPlanner.ItemRules(def.isStackable(), def.getMaxStack()),
                drop.itemDefId(), drop.rarity(), drop.quantity(), InventoryPlanner.CAPACITY);
        if (plan.isEmpty()) {
            return false;
        }

        Map<UUID, InventoryItemEntity> byId = bag.stream()
                .collect(Collectors.toMap(InventoryItemEntity::getId, Function.identity()));
        for (InventoryPlanner.StackOp op : plan.get().stacks()) {
            InventoryItemEntity stack = byId.get(op.id());
            stack.setQuantity(op.newQuantity());
            itemAuditRepository.save(new ItemAuditLogEntity(characterId, stack.getId(), drop.itemDefId(),
                    drop.rarity(), "STACK", "MOB_DROP", mobEntityId));
            out.add(new LootItem(stack.getId(), drop.itemDefId(), def.getName(), drop.rarity(), op.added()));
        }
        for (InventoryPlanner.NewSlot slot : plan.get().newSlots()) {
            InventoryItemEntity item = new InventoryItemEntity();
            item.setCharacterId(characterId);
            item.setItemDefId(drop.itemDefId());
            item.setRarity(drop.rarity());
            item.setQuantity(slot.quantity());
            item.setSlotIndex(slot.slotIndex());
            inventoryRepository.save(item);
            itemAuditRepository.save(new ItemAuditLogEntity(characterId, item.getId(), drop.itemDefId(),
                    drop.rarity(), "CREATE", "MOB_DROP", mobEntityId));
            out.add(new LootItem(item.getId(), drop.itemDefId(), def.getName(), drop.rarity(), slot.quantity()));
        }
        return true;
    }
}
