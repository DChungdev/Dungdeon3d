package com.dungeondelvers.game.instance;

import java.util.UUID;

/**
 * Persists a player's HP/MP when they leave an instance (implemented by the character service).
 * Called while instance locks are held, so implementations must only enqueue work.
 */
public interface PlayerVitalsSink {

    void save(UUID characterId, int hp, int mp);

    PlayerVitalsSink NONE = (characterId, hp, mp) -> { };
}
