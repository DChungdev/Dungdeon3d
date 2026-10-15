package com.dungeondelvers.game.instance;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A mob with the TDD §8.7 state machine:
 * <pre>
 * IDLE   → CHASE   player within aggro radius
 * CHASE  → ATTACK  target within attack range
 * ATTACK → CHASE   target moved out of range
 * CHASE  → RETURN  mob dragged beyond leash radius from spawn, or no valid target left
 * RETURN → IDLE    back at spawn (HP fully restored, so kiting to reset is pointless)
 * </pre>
 * Not thread-safe; driven by the owning room's tick.
 */
public class MobEntity extends CombatEntity {

    /** Hysteresis so a mob at the edge of its range doesn't flip ATTACK ↔ CHASE every tick. */
    static final double ATTACK_RANGE_EXIT_FACTOR = 1.15;
    /** Chasing mobs stop a little inside their range instead of walking into the target. */
    static final double CHASE_STOP_FACTOR = 0.8;
    static final double RETURN_SPEED_FACTOR = 1.5;
    /** First swing after reaching a target is delayed a little (wind-up). */
    static final long ATTACK_WINDUP_MS = 300;

    public enum AiState { IDLE, CHASE, ATTACK, RETURN, DEAD }

    /** What the AI needs from the room. */
    interface Context {
        void dealDamage(CombatEntity source, CombatEntity target, int baseDamage, double atkScaling,
                        String skillId, long nowMs);
    }

    private final CombatCatalog.MobDef def;
    private final Vec3 spawnPoint;
    /** Players who damaged this mob; they get personal loot when it dies. */
    private final Set<UUID> contributors = new LinkedHashSet<>();

    private AiState aiState = AiState.IDLE;
    private PlayerEntity target;
    private long nextAttackAtMs;
    private long diedAtMs;

    public MobEntity(CombatCatalog.MobDef def, Vec3 spawnPoint) {
        super(UUID.randomUUID(), def.name(), def.level(), def.stats(), spawnPoint);
        this.def = def;
        this.spawnPoint = spawnPoint;
    }

    void updateAi(Collection<PlayerEntity> players, double dtSec, long nowMs, Context context) {
        switch (aiState) {
            case IDLE -> {
                target = nearestAlive(players, def.aggroRadiusM());
                if (target != null) {
                    setState(AiState.CHASE);
                }
            }
            case CHASE -> {
                if (!ensureTarget(players)) {
                    return;
                }
                if (position.distanceXz(spawnPoint) > def.leashRadiusM()) {
                    target = null;
                    setState(AiState.RETURN);
                    return;
                }
                double distance = position.distanceXz(target.getPosition());
                faceTowards(target.getPosition());
                if (distance <= def.attackRangeM()) {
                    nextAttackAtMs = Math.max(nextAttackAtMs, nowMs + ATTACK_WINDUP_MS);
                    setState(AiState.ATTACK);
                } else {
                    double step = Math.min(stats.moveSpeed() * dtSec, distance - def.attackRangeM() * CHASE_STOP_FACTOR);
                    position = position.moveTowardsXz(target.getPosition(), Math.max(0, step));
                    dirty = true;
                }
            }
            case ATTACK -> {
                if (!ensureTarget(players)) {
                    return;
                }
                if (position.distanceXz(target.getPosition()) > def.attackRangeM() * ATTACK_RANGE_EXIT_FACTOR) {
                    setState(AiState.CHASE);
                    return;
                }
                faceTowards(target.getPosition());
                if (nowMs >= nextAttackAtMs) {
                    nextAttackAtMs = nowMs + (long) (1000 / Math.max(0.1, stats.attackSpeed()));
                    context.dealDamage(this, target, 0, 1.0, null, nowMs);
                }
            }
            case RETURN -> {
                double distance = position.distanceXz(spawnPoint);
                if (distance <= 0.05) {
                    position = spawnPoint;
                    hp = stats.maxHp();
                    setState(AiState.IDLE);
                } else {
                    faceTowards(spawnPoint);
                    position = position.moveTowardsXz(spawnPoint, stats.moveSpeed() * RETURN_SPEED_FACTOR * dtSec);
                }
                dirty = true;
            }
            case DEAD -> {
                // removed by the room after the despawn delay
            }
        }
    }

    /**
     * Keeps the current target if still valid, otherwise switches to the nearest alive player in
     * aggro range or goes home. Returns false if the mob has started returning.
     */
    private boolean ensureTarget(Collection<PlayerEntity> players) {
        if (target != null && target.isAlive() && players.contains(target)) {
            return true;
        }
        target = nearestAlive(players, def.aggroRadiusM());
        if (target == null) {
            setState(AiState.RETURN);
            return false;
        }
        return true;
    }

    private PlayerEntity nearestAlive(Collection<PlayerEntity> players, double radius) {
        PlayerEntity best = null;
        double bestDistance = radius;
        for (PlayerEntity player : players) {
            double distance = position.distanceXz(player.getPosition());
            if (player.isAlive() && distance <= bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Once hit, a mob fights back even if the attacker is outside its aggro radius. */
    void onAttackedBy(CombatEntity attacker) {
        if (attacker instanceof PlayerEntity) {
            contributors.add(attacker.getEntityId());
        }
        if (attacker instanceof PlayerEntity player && (aiState == AiState.IDLE || target == null)) {
            target = player;
            setState(AiState.CHASE);
        }
    }

    private void setState(AiState next) {
        if (aiState != next) {
            aiState = next;
            dirty = true;
        }
    }

    @Override
    void onDeath(long nowMs) {
        aiState = AiState.DEAD;
        target = null;
        diedAtMs = nowMs;
        dirty = true;
    }

    @Override
    String entityType() {
        return "MOB";
    }

    @Override
    String stateName() {
        return switch (aiState) {
            case IDLE -> "IDLE";
            case CHASE, RETURN -> "MOVING";
            case ATTACK -> "ATTACKING";
            case DEAD -> "DEAD";
        };
    }

    @Override
    String defId() {
        return def.id();
    }

    public AiState getAiState() {
        return aiState;
    }

    public CombatCatalog.MobDef getDef() {
        return def;
    }

    public Vec3 getSpawnPoint() {
        return spawnPoint;
    }

    Set<UUID> getContributors() {
        return contributors;
    }

    long getDiedAtMs() {
        return diedAtMs;
    }

    PlayerEntity getTarget() {
        return target;
    }
}
