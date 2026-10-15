package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.instance.InstanceManager;
import com.dungeondelvers.game.instance.InstanceRoom;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * AUTO_ATTACK_TARGET {targetEntityId}: lock an auto-attack target, or clear it with null
 * (network-protocol §8).
 */
@Component
public class AutoAttackTargetHandler implements WsMessageHandler {

    private final InstanceManager instanceManager;
    private final MessageSender messageSender;

    public AutoAttackTargetHandler(InstanceManager instanceManager, MessageSender messageSender) {
        this.instanceManager = instanceManager;
        this.messageSender = messageSender;
    }

    @Override
    public String type() {
        return MessageType.AUTO_ATTACK_TARGET;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        Optional<InstanceRoom> room = instanceManager.currentRoom(session);
        if (room.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.NOT_IN_ZONE, "Not in an instance", message.seq());
            return;
        }
        JsonNode target = message.payload().get("targetEntityId");
        UUID targetId = null;
        if (target != null && !target.isNull()) {
            targetId = HandlerSupport.parseUuid(target);
            if (targetId == null) {
                messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                        "targetEntityId must be a UUID or null", message.seq());
                return;
            }
        }
        if (!room.get().setAutoAttackTarget(session.getCharacterId(), targetId)) {
            messageSender.sendError(session, WsErrorCode.TARGET_INVALID, "Target is not an alive enemy",
                    message.seq());
        }
    }
}
