package com.dungeondelvers.game.economy;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Picks one entry from a loot table by weight (game-config-spec: loot_tables). Phase 0 drops
 * exactly one item per kill, with the entry's minimum rarity.
 */
public final class LootRoller {

    public record Entry(String itemDefId, int weight, int minQty, int maxQty, String rarity) {}

    public record Drop(String itemDefId, int quantity, String rarity) {}

    private LootRoller() {}

    public static Optional<Drop> roll(List<Entry> table, RandomGenerator random) {
        int totalWeight = table.stream().mapToInt(e -> Math.max(0, e.weight())).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }
        int pick = random.nextInt(totalWeight);
        for (Entry entry : table) {
            pick -= Math.max(0, entry.weight());
            if (pick < 0) {
                int min = Math.max(1, entry.minQty());
                int max = Math.max(min, entry.maxQty());
                int quantity = min + random.nextInt(max - min + 1);
                return Optional.of(new Drop(entry.itemDefId(), quantity, entry.rarity()));
            }
        }
        throw new IllegalStateException("unreachable: weights changed during roll");
    }
}
