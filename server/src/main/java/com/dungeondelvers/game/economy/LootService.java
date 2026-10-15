package com.dungeondelvers.game.economy;

import com.dungeondelvers.app.config.ExecutorConfig;
import com.dungeondelvers.game.instance.MobKillListener;
import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import com.dungeondelvers.game.ws.protocol.MessageType;
import com.dungeondelvers.game.ws.protocol.WsErrorCode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Personal loot (ADR-003, TDD §9.3): every player who damaged a mob gets their own roll when it
 * dies. Persistence runs on the game-persistence executor; the result is pushed as LOOT_DROP.
 */
@Service
public class LootService implements MobKillListener {

    private static final Logger log = LoggerFactory.getLogger(LootService.class);

    /** LOOT_DROP payload (network-protocol §8); reliable → ackRequired. */
    public record LootDrop(UUID mobEntityId, List<LootGrantService.LootItem> items, int gold, int goldTotal) {}

    private final LootGrantService grantService;
    private final MessageSender messageSender;
    private final ExecutorService executor;

    public LootService(LootGrantService grantService, MessageSender messageSender,
                       @Qualifier(ExecutorConfig.GAME_PERSISTENCE) ExecutorService executor) {
        this.grantService = grantService;
        this.messageSender = messageSender;
        this.executor = executor;
    }

    @Override
    public void onMobKilled(MobKill kill) {
        for (GameSession session : kill.rewarded()) {
            executor.execute(() -> grantAndNotify(kill, session));
        }
    }

    private void grantAndNotify(MobKill kill, GameSession session) {
        try {
            grantService.grant(kill.mobEntityId(), kill.mobDefId(), kill.lootTableId(), session.getCharacterId())
                    .ifPresent(result -> {
                        messageSender.send(session, MessageType.LOOT_DROP,
                                new LootDrop(kill.mobEntityId(), result.items(), result.gold(), result.goldTotal()),
                                true);
                        if (result.inventoryFull()) {
                            messageSender.sendError(session, WsErrorCode.INVENTORY_FULL,
                                    "Inventory full: item not picked up", null);
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Loot grant failed for mob {} / character {}", kill.mobEntityId(), session.getCharacterId(), ex);
        }
    }
}
