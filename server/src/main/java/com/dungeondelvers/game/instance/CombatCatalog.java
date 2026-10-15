package com.dungeondelvers.game.instance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skills and mobs usable in Phase 0, mirrored from shared/config (skills.json, mobs.json).
 * Hardcoded like {@link DungeonCatalog}; replace with a loader over the *_definitions
 * tables when more content is playable.
 */
public final class CombatCatalog {

    private CombatCatalog() {}

    public record SkillDef(
            String id,
            String classType,
            double cooldownSec,
            int mpCost,
            int baseDamage,
            double atkScaling,
            double rangeM
    ) {}

    public record MobDef(
            String id,
            String name,
            int level,
            CombatStats stats,
            double aggroRadiusM,
            double leashRadiusM,
            double attackRangeM,
            String lootTableId
    ) {}

    /** A mob placed in a room template. */
    public record MobSpawn(MobDef def, Vec3 position) {}

    /** Auto-attack (TDD §8.3): melee range; damage = ATK × 1.0 through the normal formula. */
    public static final double MELEE_AUTO_ATTACK_RANGE_M = 2.5;

    private static final Map<String, SkillDef> SKILLS = Map.of(
            "warrior_charge", new SkillDef("warrior_charge", "WARRIOR", 8.0, 15, 20, 0.8, 8.0)
    );

    // Mobs have no crit and attack once per second (not in mobs.json).
    private static final MobDef GOBLIN_GRUNT = new MobDef("mob_goblin_grunt", "Goblin Grunt", 1,
            new CombatStats(150, 0, 8, 3, 0, 1.5, 3.5, 1.0), 8.0, 20.0, 2.0, "loot_goblin_normal");
    private static final MobDef GOBLIN_ARCHER = new MobDef("mob_goblin_archer", "Goblin Archer", 3,
            new CombatStats(120, 0, 12, 2, 0, 1.5, 3.0, 1.0), 10.0, 22.0, 8.0, "loot_goblin_normal");

    /** goblin_room_1 from dungeons.json: grunt, grunt, archer — placed ahead of the player spawn. */
    private static final Map<String, List<MobSpawn>> ROOM_SPAWNS = Map.of(
            "goblin_room_1", List.of(
                    new MobSpawn(GOBLIN_GRUNT, new Vec3(-5, 0, 12)),
                    new MobSpawn(GOBLIN_GRUNT, new Vec3(5, 0, 12)),
                    new MobSpawn(GOBLIN_ARCHER, new Vec3(0, 0, 16)))
    );

    public static Optional<SkillDef> skill(String skillId) {
        return Optional.ofNullable(skillId).map(SKILLS::get);
    }

    public static List<MobSpawn> spawnsFor(String roomTemplateId) {
        return ROOM_SPAWNS.getOrDefault(roomTemplateId, List.of());
    }
}
