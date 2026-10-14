package com.dungeondelvers.game.instance;

import com.dungeondelvers.app.properties.DdoProperties;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.session.SessionExpiredEvent;
import com.dungeondelvers.game.ws.MessageSender;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Creates instance rooms, runs their tick loops and moves players in and out (TDD §4.6).
 *
 * <p>Phase 0 has no party/queue: JOIN_INSTANCE puts the player into an existing room for
 * the same dungeon + difficulty that still has space, or creates one.
 */
@Component
public class InstanceManager {

    public static final String ZONE_INSTANCE = "INSTANCE";

    private static final Logger log = LoggerFactory.getLogger(InstanceManager.class);

    /** Character data needed to spawn a player entity. */
    public record PlayerProfile(String name, int level, String classType, int maxHp, double moveSpeed) {}

    public enum JoinStatus { JOINED, ALREADY_IN_ROOM, ROOM_FULL }

    public record JoinResult(JoinStatus status, InstanceRoom room) {}

    private final Map<UUID, InstanceRoom> rooms = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> tickTasks = new ConcurrentHashMap<>();
    private final MessageSender messageSender;
    private final int maxPlayers;
    private final long tickIntervalMs;
    private final long idleTimeoutMs;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2,
            Thread.ofPlatform().name("instance-tick-", 0).daemon().factory());

    public InstanceManager(MessageSender messageSender, DdoProperties properties) {
        this.messageSender = messageSender;
        this.maxPlayers = properties.game().instanceMaxPlayers();
        this.tickIntervalMs = 1000L / properties.game().combatTickHz();
        this.idleTimeoutMs = properties.game().instanceIdleTimeoutSec() * 1000L;
    }

    /** Puts the session's character into a room for {@code dungeon}, leaving any other room first. */
    public synchronized JoinResult join(GameSession session, PlayerProfile profile,
                                        DungeonCatalog.DungeonDef dungeon, String difficulty) {
        Optional<InstanceRoom> current = currentRoom(session);
        if (current.isPresent()) {
            InstanceRoom room = current.get();
            if (room.getDungeonId().equals(dungeon.id()) && room.getDifficulty().equals(difficulty)) {
                room.sendEnterSnapshot(session);
                return new JoinResult(JoinStatus.ALREADY_IN_ROOM, room);
            }
            leave(session);
        }

        InstanceRoom room = rooms.values().stream()
                .filter(r -> r.getDungeonId().equals(dungeon.id()) && r.getDifficulty().equals(difficulty))
                .filter(InstanceRoom::hasSpace)
                .findFirst()
                .orElseGet(() -> createRoom(dungeon, difficulty));

        long now = System.currentTimeMillis();
        PlayerEntity player = new PlayerEntity(session, profile.name(), profile.level(), profile.classType(),
                profile.maxHp(), profile.moveSpeed(), room.nextSpawnPoint(), now);
        if (!room.addPlayer(player)) {
            return new JoinResult(JoinStatus.ROOM_FULL, room);
        }
        session.setZone(ZONE_INSTANCE, room.getInstanceId().toString());
        log.info("{} joined instance {} ({} players)", profile.name(), room.getInstanceId(), room.playerCount());
        return new JoinResult(JoinStatus.JOINED, room);
    }

    /** Removes the session's character from its room, if any. */
    public synchronized void leave(GameSession session) {
        currentRoom(session).ifPresent(room -> {
            room.removePlayer(session.getCharacterId());
            log.info("{} left instance {}", session.getCharacterName(), room.getInstanceId());
        });
        if (ZONE_INSTANCE.equals(session.getZoneType())) {
            session.setZone(null, null);
        }
    }

    /** The room the session is currently in, if it still exists and still contains the player. */
    public Optional<InstanceRoom> currentRoom(GameSession session) {
        if (!ZONE_INSTANCE.equals(session.getZoneType()) || session.getZoneId() == null) {
            return Optional.empty();
        }
        return findRoom(session.getZoneId())
                .filter(room -> room.containsPlayer(session.getCharacterId()));
    }

    public Optional<InstanceRoom> findRoom(String instanceId) {
        try {
            return Optional.ofNullable(rooms.get(UUID.fromString(instanceId)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public int roomCount() {
        return rooms.size();
    }

    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        leave(event.session());
    }

    @Scheduled(fixedRate = 5000)
    public void destroyIdleRooms() {
        destroyIdleRooms(System.currentTimeMillis());
    }

    synchronized void destroyIdleRooms(long nowMs) {
        for (InstanceRoom room : rooms.values()) {
            if (room.isIdleLongerThan(idleTimeoutMs, nowMs)) {
                destroyRoom(room);
            }
        }
    }

    private InstanceRoom createRoom(DungeonCatalog.DungeonDef dungeon, String difficulty) {
        int capacity = Math.min(maxPlayers, dungeon.maxPartySize());
        InstanceRoom room = new InstanceRoom(dungeon, difficulty, ThreadLocalRandom.current().nextLong(),
                capacity, messageSender, System.currentTimeMillis());
        rooms.put(room.getInstanceId(), room);
        tickTasks.put(room.getInstanceId(), scheduler.scheduleAtFixedRate(
                () -> safeTick(room), tickIntervalMs, tickIntervalMs, TimeUnit.MILLISECONDS));
        log.info("Created instance {} ({} {})", room.getInstanceId(), dungeon.id(), difficulty);
        return room;
    }

    private void destroyRoom(InstanceRoom room) {
        ScheduledFuture<?> task = tickTasks.remove(room.getInstanceId());
        if (task != null) {
            task.cancel(false);
        }
        rooms.remove(room.getInstanceId());
        for (PlayerEntity player : room.destroy()) {
            GameSession session = player.getSession();
            if (room.getInstanceId().toString().equals(session.getZoneId())) {
                session.setZone(null, null);
            }
        }
        log.info("Destroyed idle instance {}", room.getInstanceId());
    }

    private static void safeTick(InstanceRoom room) {
        try {
            room.tick(System.currentTimeMillis());
        } catch (RuntimeException ex) {
            // An exception would cancel the scheduled task and freeze the room.
            log.error("Tick failed for instance {}", room.getInstanceId(), ex);
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
