package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "item_audit_log")
public class ItemAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

    @Column(name = "item_def_id", nullable = false)
    private String itemDefId;

    @Column(nullable = false)
    private String rarity;

    /** CREATE | STACK | DELETE | TRADE | ADMIN_GRANT */
    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String reason;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ItemAuditLogEntity() {
    }

    public ItemAuditLogEntity(UUID characterId, UUID inventoryItemId, String itemDefId, String rarity,
                              String action, String reason, UUID refId) {
        this.characterId = characterId;
        this.inventoryItemId = inventoryItemId;
        this.itemDefId = itemDefId;
        this.rarity = rarity;
        this.action = action;
        this.reason = reason;
        this.refId = refId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public String getAction() {
        return action;
    }
}
