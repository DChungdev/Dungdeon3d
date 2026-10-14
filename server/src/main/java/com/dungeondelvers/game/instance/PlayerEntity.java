package com.dungeondelvers.game.instance;

import com.dungeondelvers.game.session.GameSession;
import java.util.UUID;

/**
 * Server-authoritative state of a player inside an instance. Not thread-safe on its own:
 * all access goes through the owning {@link InstanceRoom}, which serializes it.
 */
public class PlayerEntity {

    /** TDD §7.5: accept up to maxSpeed * 1.1. */
    static final double SPEED_TOLERANCE = 1.1;
    /** Absorbs float noise / tiny jitter so honest clients are never corrected. */
    static final double POSITION_EPSILON_M = 0.1;
    /** Caps the time credit a client can bank by not sending, so a pause can't become a teleport. */
    static final double MAX_MOVE_DT_SEC = 0.5;
    /** network-protocol §6: MOVE_INTENT 20 Hz max, excess dropped. */
    static final int MAX_MOVE_INTENTS_PER_SEC = 20;
    /** A MOVING player that stops sending intents is shown as IDLE after this long. */
    static final long MOVE_TIMEOUT_MS = 300;

    public enum State { IDLE, MOVING }

    /** Outcome of {@link #applyMove}. */
    public enum MoveResult { ACCEPTED, CORRECTED, RATE_LIMITED }

    private final UUID entityId;
    private final GameSession session;
    private final String name;
    private final int level;
    private final String classType;
    private final int maxHp;
    private final double moveSpeed;

    private Vec3 position;
    private double rotation;
    private State state = State.IDLE;
    private int hp;
    private long lastInputSeq;
    private long lastMoveAtMs;
    private long rateWindowStartMs;
    private int rateWindowCount;
    private boolean dirty;

    public PlayerEntity(
            GameSession session,
            String name,
            int level,
            String classType,
            int maxHp,
            double moveSpeed,
            Vec3 spawnPosition,
            long nowMs
    ) {
        this.entityId = session.getCharacterId();
        this.session = session;
        this.name = name;
        this.level = level;
        this.classType = classType;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.moveSpeed = moveSpeed;
        this.position = spawnPosition;
        this.lastMoveAtMs = nowMs;
    }

    /**
     * Applies a client movement intent. The requested position is accepted if reachable at
     * {@code moveSpeed * SPEED_TOLERANCE} since the last accepted move; otherwise the player
     * is moved as far as allowed towards it and the result is CORRECTED. Positions are
     * clamped to the room bounds.
     */
    MoveResult applyMove(Vec3 requested, double requestedRotation, Vec3 velocity, long inputSeq,
                         double roomHalfExtent, long nowMs) {
        if (nowMs - rateWindowStartMs >= 1000) {
            rateWindowStartMs = nowMs;
            rateWindowCount = 0;
        }
        if (++rateWindowCount > MAX_MOVE_INTENTS_PER_SEC) {
            return MoveResult.RATE_LIMITED;
        }

        double dtSec = Math.min(Math.max(nowMs - lastMoveAtMs, 0) / 1000.0, MAX_MOVE_DT_SEC);
        double maxDistance = moveSpeed * SPEED_TOLERANCE * dtSec + POSITION_EPSILON_M;
        Vec3 next = position.moveTowardsXz(requested, maxDistance).clampXz(roomHalfExtent);
        boolean corrected = next.distanceXz(requested) > 1e-6;

        position = next;
        rotation = normalizeRotation(requestedRotation);
        state = velocity != null && velocity.lengthXz() > 0.01 ? State.MOVING : State.IDLE;
        lastInputSeq = Math.max(lastInputSeq, inputSeq);
        lastMoveAtMs = nowMs;
        dirty = true;
        return corrected ? MoveResult.CORRECTED : MoveResult.ACCEPTED;
    }

    /** Called every tick; switches a player who stopped sending intents back to IDLE. */
    void tick(long nowMs) {
        if (state == State.MOVING && nowMs - lastMoveAtMs > MOVE_TIMEOUT_MS) {
            state = State.IDLE;
            dirty = true;
        }
    }

    /** Returns whether the entity changed since the last call, and clears the flag. */
    boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    InstancePayloads.EntitySnapshot snapshot() {
        return new InstancePayloads.EntitySnapshot(
                entityId, "PLAYER", name, level, classType, position, rotation, hp, maxHp, state.name());
    }

    InstancePayloads.EntityDelta delta() {
        return new InstancePayloads.EntityDelta(entityId, position, rotation, state.name(), lastInputSeq);
    }

    private static double normalizeRotation(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0;
        }
        double r = degrees % 360;
        return r < 0 ? r + 360 : r;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public GameSession getSession() {
        return session;
    }

    public Vec3 getPosition() {
        return position;
    }

    public State getState() {
        return state;
    }

    public long getLastInputSeq() {
        return lastInputSeq;
    }
}
