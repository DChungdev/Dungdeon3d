package com.dungeondelvers.api.rest.service;

import com.dungeondelvers.api.rest.dto.InventoryDto;
import com.dungeondelvers.common.exception.GameException;
import com.dungeondelvers.game.economy.InventoryPlanner;
import com.dungeondelvers.game.economy.ItemCatalog;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.entity.ItemDefinitionEntity;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import com.dungeondelvers.persistence.repository.InventoryItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final CharacterRepository characterRepository;
    private final InventoryItemRepository inventoryRepository;
    private final ItemCatalog itemCatalog;

    public InventoryService(CharacterRepository characterRepository, InventoryItemRepository inventoryRepository,
                            ItemCatalog itemCatalog) {
        this.characterRepository = characterRepository;
        this.inventoryRepository = inventoryRepository;
        this.itemCatalog = itemCatalog;
    }

    /** 404 (not 403) for someone else's character, so ids can't be probed. */
    @Transactional(readOnly = true)
    public InventoryDto getInventory(UUID accountId, UUID characterId) {
        CharacterEntity character = characterRepository.findByIdAndAccountId(characterId, accountId)
                .orElseThrow(() -> GameException.notFound("Character not found"));
        var items = inventoryRepository.findByCharacterIdOrderBySlotIndexAsc(characterId).stream()
                .map(item -> {
                    var def = itemCatalog.item(item.getItemDefId());
                    return new InventoryDto.Item(item.getId(), item.getItemDefId(),
                            def.map(ItemDefinitionEntity::getName).orElse(item.getItemDefId()),
                            def.map(ItemDefinitionEntity::getItemType).orElse(null),
                            item.getQuantity(), item.getRarity(), item.getEnhanceLevel(), item.isBound(),
                            item.getSlotIndex());
                })
                .toList();
        return new InventoryDto(items, InventoryPlanner.CAPACITY, character.getGold());
    }
}
