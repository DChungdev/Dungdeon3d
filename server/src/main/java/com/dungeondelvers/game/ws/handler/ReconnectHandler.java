package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.instance.InstanceManager;
import com.dungeondelvers.game.instance.InstanceRoom;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * RECONNECT (network-protocol §1.4). The session itself was already resumed by
 * SessionManager on connect; this resends the zone snapshot if the room still exists.
 */
@Component
public class ReconnectHandler implements WsMessageHandler {

    private final InstanceManager instanceManager;
    private final MessageSender messageSender;

    public ReconnectHandler(InstanceManager instanceManager, MessageSender messageSender) {
        this.instanceManager = instanceManager;
        this.messageSender = messageSender;
    }

    @Override
    public String type() {
        return MessageType.RECONNECT;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        Optional<InstanceRoom> room = instanceManager.currentRoom(session);
        if (room.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.INSTANCE_NOT_FOUND,
                    "No instance to resume", message.seq());
            return;
        }
        room.get().sendEnterSnapshot(session);
    }
}
