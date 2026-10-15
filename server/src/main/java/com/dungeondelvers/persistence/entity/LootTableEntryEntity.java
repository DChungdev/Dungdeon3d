package com.dungeondelvers.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of {@code loot_table_entries} (game config, seeded by Flyway). */
@Entity
@Table(name = "loot_table_entries")
public class LootTableEntryEntity {

    @Id
    private Integer id;

    @Column(name = "loot_table_id", nullable = false)
    private String lootTableId;

    @Column(name = "item_def_id", nullable = false)
    private String itemDefId;

    @Column(nullable = false)
    private int weight;

    @Column(name = "min_qty", nullable = false)
    private int minQty;

    @Column(name = "max_qty", nullable = false)
    private int maxQty;

    @Column(name = "min_rarity", nullable = false)
    private String minRarity;

    public Integer getId() {
        return id;
    }

    public String getLootTableId() {
        return lootTableId;
    }

    public String getItemDefId() {
        return itemDefId;
    }

    public int getWeight() {
        return weight;
    }

    public int getMinQty() {
        return minQty;
    }

    public int getMaxQty() {
        return maxQty;
    }

    public String getMinRarity() {
        return minRarity;
    }
}
