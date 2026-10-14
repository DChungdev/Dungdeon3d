package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.instance.InstanceManager;
import com.dungeondelvers.game.instance.InstanceRoom;
import com.dungeondelvers.game.instance.Vec3;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * MOVE_INTENT: the client's predicted position. The room validates it against the
 * player's speed and the result reaches every client in the next ENTITY_UPDATE.
 * Excess intents (&gt; 20/s) are dropped silently, per protocol.
 */
@Component
public class MoveIntentHandler implements WsMessageHandler {

    private final InstanceManager instanceManager;
    private final MessageSender messageSender;

    public MoveIntentHandler(InstanceManager instanceManager, MessageSender messageSender) {
        this.instanceManager = instanceManager;
        this.messageSender = messageSender;
    }

    @Override
    public String type() {
        return MessageType.MOVE_INTENT;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        JsonNode payload = message.payload();
        Vec3 position = Vec3.parse(payload.get("position"));
        JsonNode rotation = payload.get("rotation");
        JsonNode inputSeq = payload.get("inputSeq");
        if (position == null
                || rotation == null || !rotation.isNumber()
                || inputSeq == null || !inputSeq.canConvertToLong()) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                    "MOVE_INTENT requires position{x,z}, rotation and inputSeq", message.seq());
            return;
        }
        Vec3 velocity = Vec3.parse(payload.get("velocity"));

        Optional<InstanceRoom> room = instanceManager.currentRoom(session);
        if (room.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.NOT_IN_ZONE, "Not in an instance", message.seq());
            return;
        }
        room.get().applyMove(session.getCharacterId(), position, rotation.asDouble(), velocity,
                inputSeq.asLong(), System.currentTimeMillis());
    }
}
