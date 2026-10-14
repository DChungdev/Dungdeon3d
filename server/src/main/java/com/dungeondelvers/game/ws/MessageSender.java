package com.dungeondelvers.game.ws;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.Payloads;
import com.dungeondelvers.game.ws.protocol.ServerMessage;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializes and sends server envelopes. Safe to call from any thread: connections are
 * wrapped in a ConcurrentWebSocketSessionDecorator by the handler.
 */
@Component
public class MessageSender {

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    private final ObjectMapper objectMapper;

    public MessageSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void send(GameSession session, String type, Object payload) {
        send(session, type, payload, false);
    }

    public void send(GameSession session, String type, Object payload, boolean ackRequired) {
        WebSocketSession connection = session.getConnection();
        if (connection == null || !connection.isOpen()) {
            return;
        }
        ServerMessage message = new ServerMessage(
                type, session.nextServerSeq(), System.currentTimeMillis(), ackRequired, payload);
        try {
            connection.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to send {} to session {}: {}", type, session.getSessionId(), ex.getMessage());
        }
    }

    public void sendError(GameSession session, WsErrorCode code, String message, Long refSeq) {
        send(session, MessageType.ERROR, new Payloads.Error(code.name(), message, refSeq));
    }
}
