package com.dungeondelvers.game.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dungeondelvers.app.properties.DdoProperties;
import com.dungeondelvers.game.ws.MessageSender;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class SessionManagerTest {

    private final MessageSender messageSender = mock(MessageSender.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private SessionManager sessionManager;

    private final UUID accountId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        DdoProperties properties = new DdoProperties(
                null, null, new DdoProperties.GameProperties(30, 4, 300, 10, 60), null);
        sessionManager = new SessionManager(messageSender, events, properties);
    }

    @Test
    void reconnectWithinGraceResumesSameSession() {
        WebSocketSession first = connection("c1");
        GameSession session = sessionManager.open(accountId, characterId, "DelverOne", first);
        session.acceptClientSeq(10);

        sessionManager.onConnectionClosed("c1");
        when(first.isOpen()).thenReturn(false);
        assertThat(session.getState()).isEqualTo(GameSession.ConnectionState.DISCONNECTED);

        sessionManager.sweep(Instant.now().plusSeconds(30));
        GameSession resumed = sessionManager.open(accountId, characterId, "DelverOne", connection("c2"));

        assertThat(resumed.getSessionId()).isEqualTo(session.getSessionId());
        assertThat(resumed.getState()).isEqualTo(GameSession.ConnectionState.CONNECTED);
        assertThat(resumed.getLastClientSeq()).isZero();
        assertThat(sessionManager.findByConnection("c2")).contains(resumed);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void expiresSessionAfterGrace() {
        GameSession session = sessionManager.open(accountId, characterId, "DelverOne", connection("c1"));
        sessionManager.onConnectionClosed("c1");

        sessionManager.sweep(Instant.now().plusSeconds(61));

        assertThat(sessionManager.findByCharacter(characterId)).isEmpty();
        verify(events).publishEvent(new SessionExpiredEvent(session));
    }

    @Test
    void closesIdleConnection() throws Exception {
        WebSocketSession c1 = connection("c1");
        GameSession session = sessionManager.open(accountId, characterId, "DelverOne", c1);

        sessionManager.sweep(Instant.now().plusSeconds(31));

        verify(c1).close(CloseStatus.SESSION_NOT_RELIABLE);
        assertThat(session.getState()).isEqualTo(GameSession.ConnectionState.DISCONNECTED);
        assertThat(sessionManager.connectedCount()).isZero();
    }

    @Test
    void closeOfStaleConnectionDoesNotDisconnectNewOne() {
        WebSocketSession c1 = connection("c1");
        GameSession session = sessionManager.open(accountId, characterId, "DelverOne", c1);
        sessionManager.open(accountId, characterId, "DelverOne", connection("c2"));

        // The kicked connection's close callback arrives after the new one is bound.
        sessionManager.onConnectionClosed("c1");

        assertThat(session.getState()).isEqualTo(GameSession.ConnectionState.CONNECTED);
        assertThat(sessionManager.findByConnection("c2")).contains(session);
    }

    @Test
    void rejectsNonIncreasingClientSeq() {
        GameSession session = sessionManager.open(accountId, characterId, "DelverOne", connection("c1"));

        assertThat(session.acceptClientSeq(1)).isTrue();
        assertThat(session.acceptClientSeq(2)).isTrue();
        assertThat(session.acceptClientSeq(2)).isFalse();
        assertThat(session.acceptClientSeq(1)).isFalse();
    }

    private static WebSocketSession connection(String id) {
        WebSocketSession connection = mock(WebSocketSession.class);
        when(connection.getId()).thenReturn(id);
        when(connection.isOpen()).thenReturn(true);
        return connection;
    }
}
