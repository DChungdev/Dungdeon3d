package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.instance.InstanceManager;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import org.springframework.stereotype.Component;

@Component
public class LeaveInstanceHandler implements WsMessageHandler {

    private final InstanceManager instanceManager;

    public LeaveInstanceHandler(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public String type() {
        return MessageType.LEAVE_INSTANCE;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        instanceManager.leave(session);
    }
}
