package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "item_def_id", nullable = false, length = 50)
    private String itemDefId;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false, length = 20)
    private String rarity = "COMMON";

    @Column(name = "enhance_level", nullable = false)
    private int enhanceLevel;

    @Column(nullable = false)
    private boolean bound;

    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public void setCharacterId(UUID characterId) {
        this.characterId = characterId;
    }

    public String getItemDefId() {
        return itemDefId;
    }

    public void setItemDefId(String itemDefId) {
        this.itemDefId = itemDefId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public int getEnhanceLevel() {
        return enhanceLevel;
    }

    public boolean isBound() {
        return bound;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
