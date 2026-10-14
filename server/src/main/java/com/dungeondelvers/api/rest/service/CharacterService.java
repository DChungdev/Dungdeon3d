package com.dungeondelvers.api.rest.service;

import com.dungeondelvers.api.rest.dto.CharacterDto;
import com.dungeondelvers.api.rest.dto.CreateCharacterRequest;
import com.dungeondelvers.common.exception.ErrorCode;
import com.dungeondelvers.common.exception.GameException;
import com.dungeondelvers.game.CharacterBaseStats;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class CharacterService {

    private static final Set<String> ALLOWED_CLASSES = Set.of("WARRIOR", "MAGE");

    private final CharacterRepository characterRepository;
    private final ObjectMapper objectMapper;

    public CharacterService(CharacterRepository characterRepository, ObjectMapper objectMapper) {
        this.characterRepository = characterRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CharacterDto> listByAccount(UUID accountId) {
        return characterRepository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CharacterDto create(UUID accountId, CreateCharacterRequest request) {
        String classType = request.classType().trim().toUpperCase();
        if (!ALLOWED_CLASSES.contains(classType)) {
            throw GameException.badRequest(ErrorCode.INVALID_INPUT, "classType must be WARRIOR or MAGE");
        }

        String name = request.name().trim();
        if (characterRepository.existsByNameIgnoreCase(name)) {
            throw GameException.conflict(ErrorCode.NAME_TAKEN, "Character name already taken");
        }

        CharacterEntity character = new CharacterEntity();
        character.setAccountId(accountId);
        character.setName(name);
        character.setClassType(classType);
        character.setStatsJson(CharacterBaseStats.jsonForClass(classType));
        character.setHubShardId("hub-1");

        return toDto(characterRepository.save(character));
    }

    private CharacterDto toDto(CharacterEntity entity) {
        JsonNode stats;
        try {
            stats = objectMapper.readTree(entity.getStatsJson());
        } catch (Exception e) {
            stats = objectMapper.createObjectNode();
        }
        return new CharacterDto(
                entity.getId(),
                entity.getName(),
                entity.getClassType(),
                entity.getLevel(),
                entity.getXp(),
                entity.getGold(),
                entity.getPremiumGems(),
                stats,
                entity.getHubShardId(),
                List.of()
        );
    }
}
