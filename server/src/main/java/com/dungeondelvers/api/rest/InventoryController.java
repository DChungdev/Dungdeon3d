package com.dungeondelvers.api.rest;

import com.dungeondelvers.api.rest.dto.InventoryDto;
import com.dungeondelvers.api.rest.service.InventoryService;
import com.dungeondelvers.auth.jwt.SecurityUtils;
import com.dungeondelvers.common.dto.ApiResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters/{characterId}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<InventoryDto> getInventory(@PathVariable UUID characterId) {
        return ApiResponse.ok(inventoryService.getInventory(SecurityUtils.currentAccountId(), characterId));
    }
}
