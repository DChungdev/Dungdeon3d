package com.dungeondelvers.game.economy;

import com.dungeondelvers.persistence.entity.ItemDefinitionEntity;
import com.dungeondelvers.persistence.repository.ItemDefinitionRepository;
import com.dungeondelvers.persistence.repository.LootTableEntryRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Item definitions and loot tables from the config tables (seeded by Flyway), cached in memory.
 * Config only changes with a deploy, so there is no invalidation.
 */
@Component
public class ItemCatalog {

    private final ItemDefinitionRepository itemRepository;
    private final LootTableEntryRepository lootRepository;
    private final Map<String, List<LootRoller.Entry>> lootTables = new ConcurrentHashMap<>();
    private volatile Map<String, ItemDefinitionEntity> items;

    public ItemCatalog(ItemDefinitionRepository itemRepository, LootTableEntryRepository lootRepository) {
        this.itemRepository = itemRepository;
        this.lootRepository = lootRepository;
    }

    public Optional<ItemDefinitionEntity> item(String itemDefId) {
        Map<String, ItemDefinitionEntity> loaded = items;
        if (loaded == null) {
            loaded = itemRepository.findAll().stream()
                    .collect(Collectors.toUnmodifiableMap(ItemDefinitionEntity::getId, Function.identity()));
            items = loaded;
        }
        return Optional.ofNullable(loaded.get(itemDefId));
    }

    public List<LootRoller.Entry> lootTable(String lootTableId) {
        if (lootTableId == null) {
            return List.of();
        }
        return lootTables.computeIfAbsent(lootTableId, id -> lootRepository.findByLootTableIdOrderByIdAsc(id).stream()
                .map(e -> new LootRoller.Entry(e.getItemDefId(), e.getWeight(), e.getMinQty(), e.getMaxQty(),
                        e.getMinRarity()))
                .toList());
    }
}
