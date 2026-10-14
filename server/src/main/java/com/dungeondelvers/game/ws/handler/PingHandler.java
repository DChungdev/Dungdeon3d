package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.Payloads;
import org.springframework.stereotype.Component;

@Component
public class PingHandler implements WsMessageHandler {

    private final MessageSender messageSender;

    public PingHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public String type() {
        return MessageType.PING;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        messageSender.send(session, MessageType.PONG, new Payloads.Pong(System.currentTimeMillis()));
    }
}
