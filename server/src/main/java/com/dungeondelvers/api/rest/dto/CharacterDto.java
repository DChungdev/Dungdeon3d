package com.dungeondelvers.api.rest.dto;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public record CharacterDto(
        UUID id,
        String name,
        String classType,
        int level,
        long xp,
        int gold,
        int premiumGems,
        JsonNode stats,
        String hubShardId,
        List<Object> equipment
) {}
