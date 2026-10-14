package com.dungeondelvers.game.instance;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.MessageType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One dungeon room (TDD §4.6). Client intents arrive on WS threads and the tick runs on
 * the scheduler, so every public method is synchronized on the room.
 */
public class InstanceRoom {

    public enum Status { ACTIVE, DESTROYED }

    /** Phase 0 greybox room: 40m × 40m centred on the origin. */
    static final double ROOM_HALF_EXTENT_M = 20.0;

    private static final Vec3[] SPAWN_POINTS = {
            new Vec3(-2, 0, 0), new Vec3(2, 0, 0), new Vec3(0, 0, -2), new Vec3(0, 0, 2)
    };

    private final UUID instanceId = UUID.randomUUID();
    private final DungeonCatalog.DungeonDef dungeon;
    private final String difficulty;
    private final long seed;
    private final int maxPlayers;
    private final MessageSender messageSender;
    private final Map<UUID, PlayerEntity> players = new LinkedHashMap<>();

    private Status status = Status.ACTIVE;
    private long tick;
    private long lastOccupiedAtMs;

    InstanceRoom(DungeonCatalog.DungeonDef dungeon, String difficulty, long seed, int maxPlayers,
                 MessageSender messageSender, long nowMs) {
        this.dungeon = dungeon;
        this.difficulty = difficulty;
        this.seed = seed;
        this.maxPlayers = maxPlayers;
        this.messageSender = messageSender;
        this.lastOccupiedAtMs = nowMs;
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

    synchronized boolean removePlayer(UUID entityId) {
        PlayerEntity removed = players.remove(entityId);
        if (removed == null) {
            return false;
        }
        broadcastExcept(entityId, MessageType.ENTITY_DESPAWN, new InstancePayloads.EntityDespawn(entityId));
        return true;
    }

    synchronized boolean containsPlayer(UUID entityId) {
        return players.containsKey(entityId);
    }

    /** Resends the full room snapshot to one player (join or reconnect). */
    public synchronized void sendEnterSnapshot(GameSession session) {
        List<InstancePayloads.EntitySnapshot> snapshots = players.values().stream()
                .map(PlayerEntity::snapshot)
                .toList();
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

    /** 10 Hz: update entities and broadcast the changed ones as one ENTITY_UPDATE. */
    synchronized void tick(long nowMs) {
        if (status != Status.ACTIVE) {
            return;
        }
        tick++;
        List<InstancePayloads.EntityDelta> changed = new ArrayList<>();
        boolean anyConnected = false;
        for (PlayerEntity player : players.values()) {
            player.tick(nowMs);
            if (player.consumeDirty()) {
                changed.add(player.delta());
            }
            anyConnected |= player.getSession().getState() == GameSession.ConnectionState.CONNECTED;
        }
        if (anyConnected) {
            lastOccupiedAtMs = nowMs;
        }
        if (!changed.isEmpty()) {
            broadcastExcept(null, MessageType.ENTITY_UPDATE, new InstancePayloads.EntityUpdate(tick, changed));
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
}
