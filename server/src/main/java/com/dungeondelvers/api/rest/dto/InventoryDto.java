package com.dungeondelvers.api.rest.dto;

import java.util.List;
import java.util.UUID;

/** GET /characters/{id}/inventory (openapi: ApiResponseInventory) + current gold. */
public record InventoryDto(List<Item> items, int capacity, int gold) {

    public record Item(
            UUID id,
            String itemDefId,
            String name,
            String itemType,
            int quantity,
            String rarity,
            int enhanceLevel,
            boolean bound,
            int slotIndex
    ) {}
}
