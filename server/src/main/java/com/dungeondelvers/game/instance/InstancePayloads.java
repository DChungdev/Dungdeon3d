package com.dungeondelvers.game.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * Server → Client payloads for instance messages (network-protocol §4.2, §6, §8).
 */
public final class InstancePayloads {

    private InstancePayloads() {}

    /**
     * Full entity state. {@code classType} is set for players, {@code defId} (e.g.
     * mob_goblin_grunt) for mobs; {@code mp}/{@code maxMp} only for players.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EntitySnapshot(
            UUID entityId,
            String entityType,
            String name,
            int level,
            String classType,
            String defId,
            Vec3 position,
            double rotation,
            int hp,
            int maxHp,
            Integer mp,
            Integer maxMp,
            String state
    ) {}

    /**
     * One entity in an ENTITY_UPDATE batch. {@code lastInputSeq} is the last MOVE_INTENT
     * {@code inputSeq} the server applied for that player, so the owning client can
     * drop acknowledged inputs and replay the rest (reconciliation).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EntityDelta(
            UUID entityId,
            Vec3 position,
            double rotation,
            String state,
            int hp,
            Integer mp,
            Long lastInputSeq
    ) {}

    public record InstanceEnter(
            UUID instanceId,
            String dungeonId,
            String difficulty,
            long seed,
            List<String> roomTemplateIds,
            List<EntitySnapshot> entities,
            UUID localPlayerId
    ) {}

    public record EntityUpdate(long tick, List<EntityDelta> entities) {}

    public record EntitySpawn(EntitySnapshot entity) {}

    public record EntityDespawn(UUID entityId) {}

    /** {@code skillId} is null for auto-attacks. */
    public record DamageEvent(
            UUID sourceEntityId,
            UUID targetEntityId,
            int amount,
            boolean crit,
            String skillId,
            int targetHpRemaining
    ) {}

    public record EntityDeath(UUID entityId, UUID killerEntityId) {}

    /**
     * Result of SKILL_CAST for the caster only. On failure {@code reason} is SKILL_ON_COOLDOWN,
     * INSUFFICIENT_MP, TARGET_INVALID or UNKNOWN_SKILL. {@code cooldownRemainingMs} lets the
     * client show the cooldown without knowing the server tick rate.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillResult(
            String skillId,
            boolean success,
            String reason,
            long cooldownEndTick,
            long cooldownRemainingMs,
            int mpRemaining
    ) {}
}
