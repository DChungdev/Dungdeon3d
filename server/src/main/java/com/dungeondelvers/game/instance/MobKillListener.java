package com.dungeondelvers.game.instance;

import com.dungeondelvers.game.session.GameSession;
import java.util.List;
import java.util.UUID;

/**
 * Receives mob kills from instance rooms (implemented by the loot service). Called on the room's
 * tick thread while the room is locked, so implementations must only enqueue work.
 */
public interface MobKillListener {

    /**
     * @param mobEntityId runtime id of this spawn; a kill is rewarded at most once per character
     * @param rewarded    players who damaged the mob and are still in the room (personal loot, ADR-003)
     */
    record MobKill(UUID mobEntityId, String mobDefId, String lootTableId, List<GameSession> rewarded) {}

    void onMobKilled(MobKill kill);

    MobKillListener NONE = kill -> { };
}
