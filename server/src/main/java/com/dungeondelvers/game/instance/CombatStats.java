package com.dungeondelvers.game.instance;

/**
 * Final combat stats of an entity (TDD §8.1). Phase 0 has no gear or buffs, so these are
 * the base stats.
 */
public record CombatStats(
        int maxHp,
        int maxMp,
        int atk,
        int def,
        double critRate,
        double critDamage,
        double moveSpeed,
        double attackSpeed
) {}
