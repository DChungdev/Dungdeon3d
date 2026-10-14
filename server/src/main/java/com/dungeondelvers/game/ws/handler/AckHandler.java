package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import org.springframework.stereotype.Component;

/**
 * Accepts ACKs so clients don't get INVALID_MESSAGE. Retry of unacked reliable events
 * (TDD §7.8) is added together with LOOT_DROP in week 5.
 */
@Component
public class AckHandler implements WsMessageHandler {

    @Override
    public String type() {
        return MessageType.ACK;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
    }
}
