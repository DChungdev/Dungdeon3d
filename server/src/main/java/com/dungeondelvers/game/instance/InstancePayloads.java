package com.dungeondelvers.game.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * Server → Client payloads for instance messages (network-protocol §4.2, §6, §8).
 */
public final class InstancePayloads {

    private InstancePayloads() {}

    public record EntitySnapshot(
            UUID entityId,
            String entityType,
            String name,
            int level,
            String classType,
            Vec3 position,
            double rotation,
            int hp,
            int maxHp,
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
}
