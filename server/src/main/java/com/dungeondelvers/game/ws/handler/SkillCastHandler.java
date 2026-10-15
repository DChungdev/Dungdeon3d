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
 * SKILL_CAST {skillId, targetEntityId, aimPosition}. Validation failures (cooldown, MP, target)
 * come back as SKILL_RESULT success=false from the room; only malformed input is an ERROR.
 */
@Component
public class SkillCastHandler implements WsMessageHandler {

    private final InstanceManager instanceManager;
    private final MessageSender messageSender;

    public SkillCastHandler(InstanceManager instanceManager, MessageSender messageSender) {
        this.instanceManager = instanceManager;
        this.messageSender = messageSender;
    }

    @Override
    public String type() {
        return MessageType.SKILL_CAST;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        JsonNode payload = message.payload();
        JsonNode skillId = payload.get("skillId");
        if (skillId == null || !skillId.isString() || skillId.asString().isBlank()) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE, "skillId is required", message.seq());
            return;
        }
        JsonNode target = payload.get("targetEntityId");
        UUID targetId = target == null || target.isNull() ? null : HandlerSupport.parseUuid(target);
        if (target != null && !target.isNull() && targetId == null) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                    "targetEntityId must be a UUID or null", message.seq());
            return;
        }

        Optional<InstanceRoom> room = instanceManager.currentRoom(session);
        if (room.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.NOT_IN_ZONE, "Not in an instance", message.seq());
            return;
        }
        room.get().castSkill(session.getCharacterId(), skillId.asString(), targetId, System.currentTimeMillis());
    }
}
