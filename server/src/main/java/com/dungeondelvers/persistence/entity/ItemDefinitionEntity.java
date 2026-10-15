package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of {@code item_definitions} (game config, seeded by Flyway). */
@Entity
@Table(name = "item_definitions")
public class ItemDefinitionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "equip_slot")
    private String equipSlot;

    @Column(nullable = false)
    private boolean stackable;

    @Column(name = "max_stack", nullable = false)
    private int maxStack;

    @Column(name = "vendor_price", nullable = false)
    private int vendorPrice;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getItemType() {
        return itemType;
    }

    public String getEquipSlot() {
        return equipSlot;
    }

    public boolean isStackable() {
        return stackable;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getVendorPrice() {
        return vendorPrice;
    }
}
