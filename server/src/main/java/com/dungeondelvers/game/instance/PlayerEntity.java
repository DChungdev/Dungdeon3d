package com.dungeondelvers.game.instance;

import com.dungeondelvers.common.ratelimit.RateLimiter;
import com.dungeondelvers.game.session.GameSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative state of a player inside an instance. Not thread-safe on its own:
 * all access goes through the owning {@link InstanceRoom}, which serializes it.
 */
public class PlayerEntity extends CombatEntity {

    /** TDD §7.5: accept up to maxSpeed * 1.1. */
    static final double SPEED_TOLERANCE = 1.1;
    /** Absorbs float noise / tiny jitter so honest clients are never corrected. Not cumulative. */
    static final double POSITION_EPSILON_M = 0.1;
    /** Caps the movement budget a client can bank by not sending, so a pause can't become a teleport. */
    static final double MAX_MOVE_DT_SEC = 0.5;
    /** network-protocol §9: MOVE_INTENT 20 Hz max, excess dropped. */
    static final int MAX_MOVE_INTENTS_PER_SEC = 20;
    /** network-protocol §9: SKILL_CAST 5/s max, excess dropped. */
    static final int MAX_SKILL_CASTS_PER_SEC = 5;
    /** A MOVING player that stops sending intents is shown as IDLE after this long. */
    static final long MOVE_TIMEOUT_MS = 300;
    /** Phase 0: no potions yet, so MP slowly regenerates. */
    static final double MP_REGEN_PER_SEC = 2.0;
    /** Phase 0 has no revive (P2): a dead player respawns at their spawn point after this delay. */
    static final long RESPAWN_DELAY_MS = 5_000;

    public enum State { IDLE, MOVING, DEAD }

    /** Outcome of {@link #applyMove}. */
    public enum MoveResult { ACCEPTED, CORRECTED, RATE_LIMITED, DEAD }

    private final GameSession session;
    private final String classType;
    private final Vec3 spawnPoint;
    private final RateLimiter moveLimiter = new RateLimiter(MAX_MOVE_INTENTS_PER_SEC);
    private final RateLimiter skillLimiter = new RateLimiter(MAX_SKILL_CASTS_PER_SEC);
    private final Map<String, Long> cooldownUntilMs = new HashMap<>();

    private State state = State.IDLE;
    private double mp;
    private long lastInputSeq;
    private long lastMoveAtMs;
    private double moveBudgetM;
    private UUID autoAttackTargetId;
    private long nextAutoAttackAtMs;
    private long respawnAtMs;

    public PlayerEntity(
            GameSession session,
            String name,
            int level,
            String classType,
            CombatStats stats,
            Vec3 spawnPosition,
            long nowMs
    ) {
        this(session, name, level, classType, stats, null, null, spawnPosition, nowMs);
    }

    /** {@code savedHp} / {@code savedMp}: vitals from the last instance exit; null or 0 HP = full. */
    public PlayerEntity(
            GameSession session,
            String name,
            int level,
            String classType,
            CombatStats stats,
            Integer savedHp,
            Integer savedMp,
            Vec3 spawnPosition,
            long nowMs
    ) {
        super(session.getCharacterId(), name, level, stats, spawnPosition);
        this.session = session;
        this.classType = classType;
        this.spawnPoint = spawnPosition;
        if (savedHp != null && savedHp > 0) {
            this.hp = Math.min(savedHp, stats.maxHp());
        }
        this.mp = savedMp == null ? stats.maxMp() : Math.min(Math.max(savedMp, 0), stats.maxMp());
        this.lastMoveAtMs = nowMs;
    }

    /**
     * Applies a client movement intent. Elapsed time earns a distance budget at
     * {@code moveSpeed * SPEED_TOLERANCE}; the move spends it. A move beyond the budget is cut
     * short (CORRECTED). Positions are clamped to the room bounds. Dead players can't move.
     */
    MoveResult applyMove(Vec3 requested, double requestedRotation, Vec3 velocity, long inputSeq,
                         double roomHalfExtent, long nowMs) {
        if (!moveLimiter.tryAcquire(nowMs)) {
            return MoveResult.RATE_LIMITED;
        }
        if (state == State.DEAD) {
            return MoveResult.DEAD;
        }

        // Unlike a per-packet dt check, the budget tolerates packets bunched by network jitter.
        double maxSpeed = stats.moveSpeed() * SPEED_TOLERANCE;
        double dtSec = Math.max(nowMs - lastMoveAtMs, 0) / 1000.0;
        moveBudgetM = Math.min(moveBudgetM + maxSpeed * dtSec, maxSpeed * MAX_MOVE_DT_SEC);
        Vec3 next = position.moveTowardsXz(requested, moveBudgetM + POSITION_EPSILON_M).clampXz(roomHalfExtent);
        boolean corrected = next.distanceXz(requested) > 1e-6;
        // May go down to -POSITION_EPSILON_M: the tolerance is a one-off debt repaid by later
        // time, not a free 0.1 m per packet (which at 20 Hz would be +2 m/s).
        moveBudgetM -= position.distanceXz(next);

        position = next;
        rotation = normalizeRotation(requestedRotation);
        state = velocity != null && velocity.lengthXz() > 0.01 ? State.MOVING : State.IDLE;
        lastInputSeq = Math.max(lastInputSeq, inputSeq);
        lastMoveAtMs = nowMs;
        dirty = true;
        return corrected ? MoveResult.CORRECTED : MoveResult.ACCEPTED;
    }

    /** Per tick: idle timeout, MP regen, respawn. */
    void tick(long nowMs, double dtSec) {
        if (state == State.DEAD) {
            if (nowMs >= respawnAtMs) {
                respawn(nowMs);
            }
            return;
        }
        if (state == State.MOVING && nowMs - lastMoveAtMs > MOVE_TIMEOUT_MS) {
            state = State.IDLE;
            dirty = true;
        }
        if (mp < stats.maxMp()) {
            int before = (int) mp;
            mp = Math.min(stats.maxMp(), mp + MP_REGEN_PER_SEC * dtSec);
            dirty |= (int) mp != before;
        }
    }

    private void respawn(long nowMs) {
        hp = stats.maxHp();
        mp = stats.maxMp();
        position = spawnPoint;
        state = State.IDLE;
        moveBudgetM = 0;
        lastMoveAtMs = nowMs;
        dirty = true;
    }

    @Override
    void onDeath(long nowMs) {
        state = State.DEAD;
        autoAttackTargetId = null;
        respawnAtMs = nowMs + RESPAWN_DELAY_MS;
        dirty = true;
    }

    boolean tryAcquireSkillCast(long nowMs) {
        return skillLimiter.tryAcquire(nowMs);
    }

    long cooldownRemainingMs(String skillId, long nowMs) {
        return Math.max(0, cooldownUntilMs.getOrDefault(skillId, 0L) - nowMs);
    }

    void startCooldown(String skillId, long untilMs) {
        cooldownUntilMs.put(skillId, untilMs);
    }

    boolean trySpendMp(int cost) {
        if (mp < cost) {
            return false;
        }
        mp -= cost;
        dirty = true;
        return true;
    }

    /** Ready to swing: interval is 1 / attackSpeed seconds (TDD §8.3). */
    boolean autoAttackReady(long nowMs) {
        return nowMs >= nextAutoAttackAtMs;
    }

    void onAutoAttack(long nowMs) {
        nextAutoAttackAtMs = nowMs + (long) (1000 / Math.max(0.1, stats.attackSpeed()));
    }

    private static double normalizeRotation(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0;
        }
        double r = degrees % 360;
        return r < 0 ? r + 360 : r;
    }

    @Override
    String entityType() {
        return "PLAYER";
    }

    @Override
    String stateName() {
        return state.name();
    }

    @Override
    String classType() {
        return classType;
    }

    @Override
    Integer mpOrNull() {
        return (int) mp;
    }

    @Override
    Integer maxMpOrNull() {
        return stats.maxMp();
    }

    @Override
    Long lastInputSeqOrNull() {
        return lastInputSeq;
    }

    public GameSession getSession() {
        return session;
    }

    public String getClassType() {
        return classType;
    }

    public State getState() {
        return state;
    }

    public int getMp() {
        return (int) mp;
    }

    public long getLastInputSeq() {
        return lastInputSeq;
    }

    public UUID getAutoAttackTargetId() {
        return autoAttackTargetId;
    }

    void setAutoAttackTargetId(UUID targetId) {
        this.autoAttackTargetId = targetId;
    }
}
