package com.dungeondelvers.game.session;

import com.dungeondelvers.common.ratelimit.RateLimiter;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.socket.WebSocketSession;

/**
 * One logged-in character's game session. Outlives a single WebSocket connection:
 * on disconnect it stays in {@link ConnectionState#DISCONNECTED} for the reconnect
 * grace period so the player can resume (TDD §4.4).
 */
public class GameSession {

    public enum ConnectionState { CONNECTED, DISCONNECTED }

    /** All message types combined (20 MOVE_INTENT + 5 SKILL_CAST + PING/ACK/… fit easily). */
    public static final int MAX_MESSAGES_PER_SEC = 50;

    /** Outcome of {@link #admitMessage}. */
    public enum Admission { OK, DROP_AND_WARN, DROP, FLOOD }

    private final UUID sessionId = UUID.randomUUID();
    private final UUID accountId;
    private final UUID characterId;
    private final String characterName;
    private final AtomicLong serverSeq = new AtomicLong();
    private final RateLimiter messageLimiter = new RateLimiter(MAX_MESSAGES_PER_SEC);

    private volatile WebSocketSession connection;
    private volatile ConnectionState state = ConnectionState.CONNECTED;
    private volatile Instant lastMessageAt = Instant.now();
    private volatile Instant disconnectedAt;
    private volatile long lastClientSeq;
    private volatile String zoneType;
    private volatile String zoneId;

    public GameSession(UUID accountId, UUID characterId, String characterName, WebSocketSession connection) {
        this.accountId = accountId;
        this.characterId = characterId;
        this.characterName = characterName;
        this.connection = connection;
    }

    /** Attach a new connection (reconnect). Client seq restarts at 1 on each connection. */
    synchronized void rebind(WebSocketSession newConnection) {
        this.connection = newConnection;
        this.state = ConnectionState.CONNECTED;
        this.disconnectedAt = null;
        this.lastClientSeq = 0;
        this.lastMessageAt = Instant.now();
    }

    synchronized void markDisconnected() {
        this.state = ConnectionState.DISCONNECTED;
        this.disconnectedAt = Instant.now();
    }

    /**
     * Records an inbound message. Returns false when {@code seq} is not newer than the
     * last one seen on this connection (duplicate / replay), in which case it is ignored.
     */
    public synchronized boolean acceptClientSeq(long seq) {
        lastMessageAt = Instant.now();
        if (seq <= lastClientSeq) {
            return false;
        }
        lastClientSeq = seq;
        return true;
    }

    /**
     * Global per-connection rate limit (network-protocol §9). Over the limit, messages are dropped;
     * the first drop in a window gets a warning; 3× the limit in one window is treated as a flood.
     */
    public synchronized Admission admitMessage(long nowMs) {
        if (messageLimiter.tryAcquire(nowMs)) {
            return Admission.OK;
        }
        int count = messageLimiter.countInWindow();
        if (count > MAX_MESSAGES_PER_SEC * 3) {
            return Admission.FLOOD;
        }
        return count == MAX_MESSAGES_PER_SEC + 1 ? Admission.DROP_AND_WARN : Admission.DROP;
    }

    public long nextServerSeq() {
        return serverSeq.incrementAndGet();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public WebSocketSession getConnection() {
        return connection;
    }

    public ConnectionState getState() {
        return state;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    public long getLastClientSeq() {
        return lastClientSeq;
    }

    public String getZoneType() {
        return zoneType;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZone(String zoneType, String zoneId) {
        this.zoneType = zoneType;
        this.zoneId = zoneId;
    }
}
