package com.dungeondelvers.game.session;

import com.dungeondelvers.app.properties.DdoProperties;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.Payloads;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maps WebSocket connections ↔ accounts ↔ characters (TDD §4.4).
 *
 * <p>Phase 0 keeps sessions in-process ({@code ddo.cache.type=memory}); a Redis-backed
 * store is needed once there is more than one game-server node.
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    /** Server closes a connection that has sent nothing for this long (network-protocol §1.3). */
    static final Duration IDLE_TIMEOUT = Duration.ofSeconds(30);

    private final Map<String, GameSession> byConnectionId = new ConcurrentHashMap<>();
    private final Map<UUID, GameSession> byCharacterId = new ConcurrentHashMap<>();

    private final MessageSender messageSender;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration reconnectGrace;

    public SessionManager(
            MessageSender messageSender,
            ApplicationEventPublisher eventPublisher,
            DdoProperties properties
    ) {
        this.messageSender = messageSender;
        this.eventPublisher = eventPublisher;
        this.reconnectGrace = Duration.ofSeconds(properties.game().reconnectGraceSec());
    }

    /**
     * Opens a session for a new connection. If the character already has a session it is
     * resumed (reconnect within grace); a still-open old connection is kicked.
     */
    public GameSession open(UUID accountId, UUID characterId, String characterName, WebSocketSession connection) {
        GameSession session = byCharacterId.compute(characterId, (id, existing) -> {
            if (existing == null) {
                return new GameSession(accountId, characterId, characterName, connection);
            }
            WebSocketSession old = existing.getConnection();
            if (old != null && old.isOpen()) {
                byConnectionId.remove(old.getId());
                messageSender.send(existing, MessageType.KICK,
                        new Payloads.Kick("DUPLICATE_LOGIN", "Character logged in from another connection"));
                closeQuietly(old, CloseStatus.POLICY_VIOLATION);
            }
            existing.rebind(connection);
            return existing;
        });
        byConnectionId.put(connection.getId(), session);
        return session;
    }

    public Optional<GameSession> findByConnection(String connectionId) {
        return Optional.ofNullable(byConnectionId.get(connectionId));
    }

    public Optional<GameSession> findByCharacter(UUID characterId) {
        return Optional.ofNullable(byCharacterId.get(characterId));
    }

    /** Number of currently connected players (for /health CCU). */
    public int connectedCount() {
        return byConnectionId.size();
    }

    Collection<GameSession> allSessions() {
        return byCharacterId.values();
    }

    /** Called when a connection closes. The session is kept for the reconnect grace period. */
    public void onConnectionClosed(String connectionId) {
        GameSession session = byConnectionId.remove(connectionId);
        if (session == null) {
            return;
        }
        WebSocketSession current = session.getConnection();
        // Only mark disconnected if this was still the session's active connection.
        if (current != null && current.getId().equals(connectionId)) {
            session.markDisconnected();
            log.debug("Session {} disconnected, grace {}s", session.getSessionId(), reconnectGrace.toSeconds());
        }
    }

    @Scheduled(fixedRate = 1000)
    public void sweep() {
        sweep(Instant.now());
    }

    void sweep(Instant now) {
        for (GameSession session : byCharacterId.values()) {
            if (session.getState() == GameSession.ConnectionState.CONNECTED) {
                if (session.getLastMessageAt().plus(IDLE_TIMEOUT).isBefore(now)) {
                    log.debug("Session {} idle > {}s, closing", session.getSessionId(), IDLE_TIMEOUT.toSeconds());
                    WebSocketSession connection = session.getConnection();
                    if (connection != null) {
                        closeQuietly(connection, CloseStatus.SESSION_NOT_RELIABLE);
                        onConnectionClosed(connection.getId());
                    }
                }
            } else if (session.getDisconnectedAt() != null
                    && session.getDisconnectedAt().plus(reconnectGrace).isBefore(now)) {
                if (byCharacterId.remove(session.getCharacterId(), session)) {
                    log.debug("Session {} expired", session.getSessionId());
                    eventPublisher.publishEvent(new SessionExpiredEvent(session));
                }
            }
        }
    }

    private static void closeQuietly(WebSocketSession connection, CloseStatus status) {
        try {
            connection.close(status);
        } catch (IOException ignored) {
            // connection already broken
        }
    }
}
