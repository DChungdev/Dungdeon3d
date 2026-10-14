package com.dungeondelvers.game;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class CharacterBaseStats {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private CharacterBaseStats() {}

    public static Map<String, Object> forClass(String classType) {
        return switch (classType) {
            case "WARRIOR" -> Map.of(
                    "maxHp", 1000,
                    "maxMp", 100,
                    "atk", 50,
                    "def", 30,
                    "critRate", 0.05,
                    "critDamage", 1.5,
                    "moveSpeed", 5.0,
                    "attackSpeed", 1.0
            );
            case "MAGE" -> Map.of(
                    "maxHp", 700,
                    "maxMp", 200,
                    "atk", 55,
                    "def", 20,
                    "critRate", 0.08,
                    "critDamage", 1.5,
                    "moveSpeed", 5.0,
                    "attackSpeed", 0.9
            );
            default -> throw new IllegalArgumentException("Unknown classType: " + classType);
        };
    }

    public static String jsonForClass(String classType) {
        return MAPPER.writeValueAsString(forClass(classType));
    }
}
