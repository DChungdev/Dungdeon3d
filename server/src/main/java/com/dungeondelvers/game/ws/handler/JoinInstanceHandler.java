package com.dungeondelvers.game.ws.handler;

import com.dungeondelvers.game.instance.DungeonCatalog;
import com.dungeondelvers.game.instance.InstanceManager;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.ClientMessage;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * JOIN_INSTANCE (network-protocol §8): Phase 0 direct join, bypassing party / queue.
 */
@Component
public class JoinInstanceHandler implements WsMessageHandler {

    private static final Set<String> DIFFICULTIES = Set.of("NORMAL", "HARD");

    private final InstanceManager instanceManager;
    private final DungeonCatalog dungeonCatalog;
    private final CharacterRepository characterRepository;
    private final MessageSender messageSender;
    private final ObjectMapper objectMapper;

    public JoinInstanceHandler(
            InstanceManager instanceManager,
            DungeonCatalog dungeonCatalog,
            CharacterRepository characterRepository,
            MessageSender messageSender,
            ObjectMapper objectMapper
    ) {
        this.instanceManager = instanceManager;
        this.dungeonCatalog = dungeonCatalog;
        this.characterRepository = characterRepository;
        this.messageSender = messageSender;
        this.objectMapper = objectMapper;
    }

    @Override
    public String type() {
        return MessageType.JOIN_INSTANCE;
    }

    @Override
    public void handle(GameSession session, ClientMessage message) {
        JsonNode payload = message.payload();
        String dungeonId = textOrNull(payload.get("dungeonId"));
        String difficulty = Optional.ofNullable(textOrNull(payload.get("difficulty"))).orElse("NORMAL");

        if (!DIFFICULTIES.contains(difficulty)) {
            messageSender.sendError(session, WsErrorCode.INVALID_MESSAGE,
                    "difficulty must be NORMAL or HARD", message.seq());
            return;
        }
        Optional<DungeonCatalog.DungeonDef> dungeon = dungeonCatalog.find(dungeonId);
        if (dungeon.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.INSTANCE_NOT_FOUND,
                    "Unknown or unavailable dungeon: " + dungeonId, message.seq());
            return;
        }
        Optional<CharacterEntity> character = characterRepository.findById(session.getCharacterId());
        if (character.isEmpty()) {
            messageSender.sendError(session, WsErrorCode.UNAUTHORIZED, "Character not found", message.seq());
            return;
        }

        InstanceManager.JoinResult result = instanceManager.join(
                session, toProfile(character.get()), dungeon.get(), difficulty);
        if (result.status() == InstanceManager.JoinStatus.ROOM_FULL) {
            messageSender.sendError(session, WsErrorCode.INSTANCE_NOT_FOUND, "Instance is full", message.seq());
        }
    }

    private InstanceManager.PlayerProfile toProfile(CharacterEntity character) {
        int maxHp = 1000;
        double moveSpeed = 5.0;
        try {
            JsonNode stats = objectMapper.readTree(character.getStatsJson());
            maxHp = stats.path("maxHp").asInt(maxHp);
            moveSpeed = stats.path("moveSpeed").asDouble(moveSpeed);
        } catch (JacksonException ignored) {
            // fall back to defaults
        }
        return new InstanceManager.PlayerProfile(
                character.getName(), character.getLevel(), character.getClassType(), maxHp, moveSpeed);
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isString() ? node.asString() : null;
    }
}
