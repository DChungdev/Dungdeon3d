package com.dungeondelvers.game.instance;

import java.util.UUID;

/**
 * Anything with HP inside an instance: players and mobs. Not thread-safe; access is
 * serialized by the owning {@link InstanceRoom}.
 */
public abstract class CombatEntity {

    protected final UUID entityId;
    protected final String name;
    protected final int level;
    protected final CombatStats stats;

    protected Vec3 position;
    protected double rotation;
    protected int hp;
    protected boolean dirty;

    protected CombatEntity(UUID entityId, String name, int level, CombatStats stats, Vec3 position) {
        this.entityId = entityId;
        this.name = name;
        this.level = level;
        this.stats = stats;
        this.position = position;
        this.hp = stats.maxHp();
    }

    /** PLAYER | MOB (network-protocol §4.2). */
    abstract String entityType();

    /** IDLE | MOVING | ATTACKING | DEAD. */
    abstract String stateName();

    String classType() {
        return null;
    }

    String defId() {
        return null;
    }

    Integer mpOrNull() {
        return null;
    }

    Integer maxMpOrNull() {
        return null;
    }

    Long lastInputSeqOrNull() {
        return null;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    /** Reduces HP; returns true if this hit killed the entity. Dead entities ignore damage. */
    boolean takeDamage(int amount) {
        if (!isAlive()) {
            return false;
        }
        hp = Math.max(0, hp - amount);
        dirty = true;
        return hp == 0;
    }

    /** Called once when HP reaches 0. */
    abstract void onDeath(long nowMs);

    /** Returns whether the entity changed since the last call, and clears the flag. */
    boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    void faceTowards(Vec3 target) {
        double dx = target.x() - position.x();
        double dz = target.z() - position.z();
        if (dx * dx + dz * dz > 1e-6) {
            // yaw in degrees, 0 = +Z, 90 = +X (Unity convention)
            double yaw = Math.toDegrees(Math.atan2(dx, dz));
            yaw = yaw < 0 ? yaw + 360 : yaw;
            if (Math.abs(yaw - rotation) > 0.5) {
                rotation = yaw;
                dirty = true;
            }
        }
    }

    InstancePayloads.EntitySnapshot snapshot() {
        return new InstancePayloads.EntitySnapshot(entityId, entityType(), name, level, classType(), defId(),
                position, rotation, hp, stats.maxHp(), mpOrNull(), maxMpOrNull(), stateName());
    }

    InstancePayloads.EntityDelta delta() {
        return new InstancePayloads.EntityDelta(entityId, position, rotation, stateName(), hp, mpOrNull(),
                lastInputSeqOrNull());
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public CombatStats getStats() {
        return stats;
    }

    public Vec3 getPosition() {
        return position;
    }

    public int getHp() {
        return hp;
    }
}
