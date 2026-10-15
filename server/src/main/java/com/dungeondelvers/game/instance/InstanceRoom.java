package com.dungeondelvers.game.instance;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.MessageType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * One dungeon room (TDD §4.6) with its 10 Hz combat loop (TDD §4.7). Client intents arrive on
 * WS threads and the tick runs on the scheduler, so every public method is synchronized on the room.
 */
public class InstanceRoom implements MobEntity.Context {

    public enum Status { ACTIVE, DESTROYED }

    /** Phase 0 greybox room: 40m × 40m centred on the origin. */
    static final double ROOM_HALF_EXTENT_M = 20.0;
    /** Extra range accepted for player attacks: client and server positions differ by latency. */
    static final double RANGE_TOLERANCE_M = 0.5;
    /** Dead mobs stay visible this long (death animation) before ENTITY_DESPAWN. */
    static final long MOB_DESPAWN_DELAY_MS = 2_000;
    /** Phase 0 convenience: killed mobs come back so the room can be tested repeatedly. */
    static final long MOB_RESPAWN_DELAY_MS = 20_000;

    private static final Vec3[] SPAWN_POINTS = {
            new Vec3(-2, 0, 0), new Vec3(2, 0, 0), new Vec3(0, 0, -2), new Vec3(0, 0, 2)
    };

    private record PendingRespawn(CombatCatalog.MobSpawn spawn, long atMs) {}

    private final UUID instanceId = UUID.randomUUID();
    private final DungeonCatalog.DungeonDef dungeon;
    private final String difficulty;
    private final long seed;
    private final int maxPlayers;
    private final long tickIntervalMs;
    private final MessageSender messageSender;
    private final RandomGenerator random;
    private final MobKillListener mobKillListener;
    private final Map<UUID, PlayerEntity> players = new LinkedHashMap<>();
    private final Map<UUID, MobEntity> mobs = new LinkedHashMap<>();
    private final List<PendingRespawn> pendingRespawns = new ArrayList<>();

    private Status status = Status.ACTIVE;
    private long tick;
    private long lastTickAtMs;
    private long lastOccupiedAtMs;

    InstanceRoom(DungeonCatalog.DungeonDef dungeon, String difficulty, long seed, int maxPlayers,
                 long tickIntervalMs, MessageSender messageSender, RandomGenerator random,
                 MobKillListener mobKillListener, long nowMs) {
        this.dungeon = dungeon;
        this.difficulty = difficulty;
        this.seed = seed;
        this.maxPlayers = maxPlayers;
        this.tickIntervalMs = tickIntervalMs;
        this.messageSender = messageSender;
        this.random = random;
        this.mobKillListener = mobKillListener;
        this.lastOccupiedAtMs = nowMs;
        this.lastTickAtMs = nowMs;
        // Phase 0: a single greybox room populated with the first room template's mobs.
        for (CombatCatalog.MobSpawn spawn : CombatCatalog.spawnsFor(dungeon.roomTemplateIds().get(0))) {
            MobEntity mob = new MobEntity(spawn.def(), spawn.position());
            mobs.put(mob.getEntityId(), mob);
        }
    }

    synchronized boolean hasSpace() {
        return status == Status.ACTIVE && players.size() < maxPlayers;
    }

    /** Next free spawn point; players are spread so they don't stack on join. */
    synchronized Vec3 nextSpawnPoint() {
        return SPAWN_POINTS[players.size() % SPAWN_POINTS.length];
    }

    /**
     * Adds a player, sends them the INSTANCE_ENTER snapshot and announces them to everyone
     * else. Returns false if the room is full or destroyed.
     */
    synchronized boolean addPlayer(PlayerEntity player) {
        if (!hasSpace()) {
            return false;
        }
        players.put(player.getEntityId(), player);
        player.consumeDirty();
        sendEnterSnapshot(player.getSession());
        broadcastExcept(player.getEntityId(), MessageType.ENTITY_SPAWN,
                new InstancePayloads.EntitySpawn(player.snapshot()));
        return true;
    }

    /** Removes a player and returns their final state (for saving HP/MP). */
    synchronized Optional<PlayerEntity> removePlayer(UUID entityId) {
        PlayerEntity removed = players.remove(entityId);
        if (removed == null) {
            return Optional.empty();
        }
        removed.setAutoAttackTargetId(null);
        broadcastExcept(entityId, MessageType.ENTITY_DESPAWN, new InstancePayloads.EntityDespawn(entityId));
        return Optional.of(removed);
    }

    synchronized boolean containsPlayer(UUID entityId) {
        return players.containsKey(entityId);
    }

    /** Resends the full room snapshot to one player (join or reconnect). */
    public synchronized void sendEnterSnapshot(GameSession session) {
        List<InstancePayloads.EntitySnapshot> snapshots = new ArrayList<>();
        players.values().forEach(p -> snapshots.add(p.snapshot()));
        mobs.values().forEach(m -> snapshots.add(m.snapshot()));
        messageSender.send(session, MessageType.INSTANCE_ENTER, new InstancePayloads.InstanceEnter(
                instanceId, dungeon.id(), difficulty, seed, dungeon.roomTemplateIds(), snapshots,
                session.getCharacterId()));
    }

    public synchronized Optional<PlayerEntity.MoveResult> applyMove(
            UUID entityId, Vec3 position, double rotation, Vec3 velocity, long inputSeq, long nowMs) {
        PlayerEntity player = players.get(entityId);
        if (player == null || status != Status.ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(player.applyMove(position, rotation, velocity, inputSeq, ROOM_HALF_EXTENT_M, nowMs));
    }

    /**
     * AUTO_ATTACK_TARGET: locks (or with null clears) the player's auto-attack target. Range is
     * checked on every swing, not here, so the player can lock first and walk up.
     * Returns false if the target is not an alive mob in this room.
     */
    public synchronized boolean setAutoAttackTarget(UUID playerId, UUID targetId) {
        PlayerEntity player = players.get(playerId);
        if (player == null) {
            return false;
        }
        if (targetId == null) {
            player.setAutoAttackTargetId(null);
            return true;
        }
        MobEntity mob = mobs.get(targetId);
        if (mob == null || !mob.isAlive() || !player.isAlive()) {
            return false;
        }
        player.setAutoAttackTargetId(targetId);
        return true;
    }

    /**
     * SKILL_CAST pipeline (TDD §8.4): cooldown → MP → target → effect → cooldown start. Sends
     * SKILL_RESULT to the caster and returns it; returns empty if the cast was rate limited
     * (dropped silently, network-protocol §9) or the player is not in this room.
     */
    public synchronized Optional<InstancePayloads.SkillResult> castSkill(
            UUID playerId, String skillId, UUID targetId, long nowMs) {
        PlayerEntity player = players.get(playerId);
        if (player == null || status != Status.ACTIVE || !player.tryAcquireSkillCast(nowMs)) {
            return Optional.empty();
        }
        InstancePayloads.SkillResult result = resolveSkill(player, skillId, targetId, nowMs);
        messageSender.send(player.getSession(), MessageType.SKILL_RESULT, result);
        return Optional.of(result);
    }

    private InstancePayloads.SkillResult resolveSkill(PlayerEntity player, String skillId, UUID targetId,
                                                      long nowMs) {
        CombatCatalog.SkillDef skill = CombatCatalog.skill(skillId)
                .filter(s -> s.classType().equals(player.getClassType()))
                .orElse(null);
        if (skill == null) {
            return skillFailure(player, skillId, "UNKNOWN_SKILL", nowMs);
        }
        if (!player.isAlive()) {
            return skillFailure(player, skillId, "TARGET_INVALID", nowMs);
        }
        if (player.cooldownRemainingMs(skillId, nowMs) > 0) {
            return skillFailure(player, skillId, "SKILL_ON_COOLDOWN", nowMs);
        }
        if (player.getMp() < skill.mpCost()) {
            return skillFailure(player, skillId, "INSUFFICIENT_MP", nowMs);
        }
        MobEntity target = targetId == null ? null : mobs.get(targetId);
        if (target == null || !target.isAlive()
                || player.getPosition().distanceXz(target.getPosition()) > skill.rangeM() + RANGE_TOLERANCE_M) {
            return skillFailure(player, skillId, "TARGET_INVALID", nowMs);
        }

        player.trySpendMp(skill.mpCost());
        long cooldownMs = (long) (skill.cooldownSec() * 1000);
        player.startCooldown(skillId, nowMs + cooldownMs);
        // Phase 0: Charge deals its damage; the dash/knockback movement needs server-driven
        // movement in client reconciliation and is deferred.
        dealDamage(player, target, skill.baseDamage(), skill.atkScaling(), skillId, nowMs);
        return new InstancePayloads.SkillResult(skillId, true, null, cooldownEndTick(cooldownMs), cooldownMs,
                player.getMp());
    }

    private InstancePayloads.SkillResult skillFailure(PlayerEntity player, String skillId, String reason,
                                                     long nowMs) {
        long remaining = player.cooldownRemainingMs(skillId, nowMs);
        return new InstancePayloads.SkillResult(skillId, false, reason, cooldownEndTick(remaining), remaining,
                player.getMp());
    }

    private long cooldownEndTick(long remainingMs) {
        return tick + (remainingMs + tickIntervalMs - 1) / tickIntervalMs;
    }

    /** Applies one hit through the damage formula and broadcasts DAMAGE_EVENT (+ ENTITY_DEATH). */
    @Override
    public void dealDamage(CombatEntity source, CombatEntity target, int baseDamage, double atkScaling,
                           String skillId, long nowMs) {
        DamageCalculator.Hit hit = DamageCalculator.calculate(
                baseDamage, atkScaling, source.getStats(), target.getStats(), random);
        boolean killed = target.takeDamage(hit.amount());
        broadcastExcept(null, MessageType.DAMAGE_EVENT, new InstancePayloads.DamageEvent(
                source.getEntityId(), target.getEntityId(), hit.amount(), hit.crit(), skillId, target.getHp()));
        if (target instanceof MobEntity mob) {
            mob.onAttackedBy(source);
        }
        if (killed) {
            target.onDeath(nowMs);
            broadcastExcept(null, MessageType.ENTITY_DEATH,
                    new InstancePayloads.EntityDeath(target.getEntityId(), source.getEntityId()));
            if (target instanceof MobEntity mob) {
                List<GameSession> rewarded = mob.getContributors().stream()
                        .map(players::get)
                        .filter(Objects::nonNull)
                        .map(PlayerEntity::getSession)
                        .toList();
                mobKillListener.onMobKilled(new MobKillListener.MobKill(
                        mob.getEntityId(), mob.getDef().id(), mob.getDef().lootTableId(), rewarded));
            }
        }
    }

    /**
     * 10 Hz tick (TDD §4.7): players (regen/respawn) → player auto-attacks → mob AI →
     * despawn/respawn → broadcast changed entities as one ENTITY_UPDATE.
     */
    synchronized void tick(long nowMs) {
        if (status != Status.ACTIVE) {
            return;
        }
        tick++;
        double dtSec = Math.min(Math.max(nowMs - lastTickAtMs, 0), 1000) / 1000.0;
        lastTickAtMs = nowMs;

        boolean anyConnected = false;
        for (PlayerEntity player : players.values()) {
            player.tick(nowMs, dtSec);
            anyConnected |= player.getSession().getState() == GameSession.ConnectionState.CONNECTED;
        }
        if (anyConnected) {
            lastOccupiedAtMs = nowMs;
        }

        for (PlayerEntity player : players.values()) {
            tickAutoAttack(player, nowMs);
        }
        for (MobEntity mob : mobs.values()) {
            mob.updateAi(players.values(), dtSec, nowMs, this);
        }
        tickMobLifecycle(nowMs);

        List<InstancePayloads.EntityDelta> changed = new ArrayList<>();
        for (PlayerEntity player : players.values()) {
            if (player.consumeDirty()) {
                changed.add(player.delta());
            }
        }
        for (MobEntity mob : mobs.values()) {
            if (mob.consumeDirty()) {
                changed.add(mob.delta());
            }
        }
        if (!changed.isEmpty()) {
            broadcastExcept(null, MessageType.ENTITY_UPDATE, new InstancePayloads.EntityUpdate(tick, changed));
        }
    }

    private void tickAutoAttack(PlayerEntity player, long nowMs) {
        UUID targetId = player.getAutoAttackTargetId();
        if (targetId == null || !player.isAlive()) {
            return;
        }
        MobEntity target = mobs.get(targetId);
        if (target == null || !target.isAlive()) {
            player.setAutoAttackTargetId(null);
            return;
        }
        double range = CombatCatalog.MELEE_AUTO_ATTACK_RANGE_M + RANGE_TOLERANCE_M;
        if (player.autoAttackReady(nowMs) && player.getPosition().distanceXz(target.getPosition()) <= range) {
            player.onAutoAttack(nowMs);
            dealDamage(player, target, 0, 1.0, null, nowMs);
        }
    }

    private void tickMobLifecycle(long nowMs) {
        for (Iterator<MobEntity> it = mobs.values().iterator(); it.hasNext(); ) {
            MobEntity mob = it.next();
            if (!mob.isAlive() && nowMs - mob.getDiedAtMs() >= MOB_DESPAWN_DELAY_MS) {
                it.remove();
                broadcastExcept(null, MessageType.ENTITY_DESPAWN, new InstancePayloads.EntityDespawn(mob.getEntityId()));
                pendingRespawns.add(new PendingRespawn(
                        new CombatCatalog.MobSpawn(mob.getDef(), mob.getSpawnPoint()), nowMs + MOB_RESPAWN_DELAY_MS));
            }
        }
        for (Iterator<PendingRespawn> it = pendingRespawns.iterator(); it.hasNext(); ) {
            PendingRespawn pending = it.next();
            if (nowMs >= pending.atMs()) {
                it.remove();
                MobEntity mob = new MobEntity(pending.spawn().def(), pending.spawn().position());
                mobs.put(mob.getEntityId(), mob);
                broadcastExcept(null, MessageType.ENTITY_SPAWN, new InstancePayloads.EntitySpawn(mob.snapshot()));
            }
        }
    }

    synchronized boolean isIdleLongerThan(long idleMs, long nowMs) {
        return nowMs - lastOccupiedAtMs > idleMs;
    }

    /** Marks the room destroyed and returns the players that were still in it. */
    synchronized List<PlayerEntity> destroy() {
        status = Status.DESTROYED;
        List<PlayerEntity> remaining = new ArrayList<>(players.values());
        players.clear();
        mobs.clear();
        pendingRespawns.clear();
        return remaining;
    }

    private void broadcastExcept(UUID excludedEntityId, String type, Object payload) {
        for (PlayerEntity player : players.values()) {
            if (!player.getEntityId().equals(excludedEntityId)) {
                messageSender.send(player.getSession(), type, payload);
            }
        }
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getDungeonId() {
        return dungeon.id();
    }

    public String getDifficulty() {
        return difficulty;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized int playerCount() {
        return players.size();
    }

    synchronized long currentTick() {
        return tick;
    }

    synchronized List<MobEntity> mobs() {
        return List.copyOf(mobs.values());
    }

    synchronized Optional<PlayerEntity> player(UUID entityId) {
        return Optional.ofNullable(players.get(entityId));
    }
}
