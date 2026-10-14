package com.dungeondelvers.game.instance;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Dungeons that can be entered via JOIN_INSTANCE.
 *
 * <p>Phase 0 hardcodes Goblin Cave (mirrors shared/config/dungeons.json). Replace with a
 * loader over {@code dungeon_definitions} once more than one dungeon is playable.
 */
@Component
public class DungeonCatalog {

    public record DungeonDef(String id, int maxPartySize, List<String> roomTemplateIds) {}

    private static final Map<String, DungeonDef> DUNGEONS = Map.of(
            "dungeon_goblin_cave",
            new DungeonDef("dungeon_goblin_cave", 4, List.of("goblin_room_1", "goblin_room_2", "goblin_boss"))
    );

    public Optional<DungeonDef> find(String dungeonId) {
        return Optional.ofNullable(dungeonId).map(DUNGEONS::get);
    }
}
