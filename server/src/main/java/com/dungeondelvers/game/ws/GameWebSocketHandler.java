package com.dungeondelvers.game.ws;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.session.SessionManager;
import com.dungeondelvers.game.ws.handler.WsMessageHandler;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.Payloads;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Entry point for {@code /ws/game}: parses envelopes, drops replayed seqs and
 * dispatches to the {@link WsMessageHandler} registered for the message type.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    public static final String CONFIG_VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    /** network-protocol.md: max message size 64 KB. Larger frames close the connection (1009). */
    private static final int MAX_MESSAGE_BYTES = 64 * 1024;
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

    private final SessionManager sessionManager;
    private final MessageSender messageSender;
    private final ObjectMapper objectMapper;
    private final Map<String, WsMessageHandler> handlers;

    public GameWebSocketHandler(
            SessionManager sessionManager,
            MessageSender messageSender,
            ObjectMapper objectMapper,
            List<WsMessageHandler> handlers
    ) {
        this.sessionManager = sessionManager;
        this.messageSender = messageSender;
        this.objectMapper = objectMapper;
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(WsMessageHandler::type, Function.identity()));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawConnection) {
        rawConnection.setTextMessageSizeLimit(MAX_MESSAGE_BYTES);
        rawConnection.setBinaryMessageSizeLimit(MAX_MESSAGE_BYTES);
        var connection = new ConcurrentWebSocketSessionDecorator(
                rawConnection, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
        Map<String, Object> attrs = rawConnection.getAttributes();
        UUID accountId = (UUID) attrs.get(JwtHandshakeInterceptor.ATTR_ACCOUNT_ID);
        UUID characterId = (UUID) attrs.get(JwtHandshakeInterceptor.ATTR_CHARACTER_ID);
        String characterName = (String) attrs.get(JwtHandshakeInterceptor.ATTR_CHARACTER_NAME);

        GameSession session = sessionManager.open(accountId, characterId, characterName, connection);
        log.info("WS connected: session={} character={}", session.getSessionId(), characterName);

        messageSender.send(session, MessageType.CONNECTED, new Payloads.Connected(
                session.getSessionId(), System.currentTimeMillis(), CONFIG_VERSION, characterId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession connection, TextMessage textMessage) {
        GameSession session = sessionManager.findByConnection(connection.getId()).orElse(null);
        if (session == null) {
            return;
        }

        switch (session.admitMessage(System.currentTimeMillis())) {
            case OK -> { }
            case DROP_AND_WARN -> {
                messageSender.sendError(session, WsErrorCode.RATE_LIMITED, "Too many messages", null);
                return;
            }
            case DROP -> {
                return;
            }
            case FLOOD -> {
                log.warn("Closing flooding connection for session {}", session.getSessionId());
                closeQuietly(connection, CloseStatus.POLICY_VIOLATION);
                return;
            }
        }

        ClientMessage message;
        try {
            message = objectMapper.readValue(textMessage.getPayload(), ClientMessage.class);
        } catch (JacksonException ex) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE, "Malformed JSON envelope", null);
            return;
        }
        if (!message.isWellFormed()) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                    "Envelope requires type, seq > 0 and object payload", message.seq() > 0 ? message.seq() : null);
            return;
        }
        if (!session.acceptClientSeq(message.seq())) {
            log.debug("Dropping duplicate seq {} from session {}", message.seq(), session.getSessionId());
            return;
        }

        WsMessageHandler handler = handlers.get(message.type());
        if (handler == null) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                    "Unknown message type: " + message.type(), message.seq());
            return;
        }
        try {
            handler.handle(session, message);
        } catch (RuntimeException ex) {
            log.error("Handler {} failed for session {}", message.type(), session.getSessionId(), ex);
            messageSender.sendError(session, WsErrorCode.INTERNAL_ERROR, "Internal server error", message.seq());
        }
    }

    private static void closeQuietly(WebSocketSession connection, CloseStatus status) {
        try {
            connection.close(status);
        } catch (IOException ignored) {
            // already broken
        }
    }

    @Override
    public void handleTransportError(WebSocketSession connection, Throwable exception) {
        log.debug("WS transport error on {}: {}", connection.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession connection, CloseStatus status) {
        log.info("WS closed: connection={} status={}", connection.getId(), status);
        sessionManager.onConnectionClosed(connection.getId());
    }
}
