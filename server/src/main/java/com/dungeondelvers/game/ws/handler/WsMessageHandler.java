package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.protocol.ClientMessage;

/**
 * Handles one client message type. Implementations are Spring beans and are picked up
 * automatically by {@link com.dungeondelvers.game.ws.GameWebSocketHandler}.
 */
public interface WsMessageHandler {

    String type();

    void handle(GameSession session, ClientMessage message);
}
