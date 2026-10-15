package com.dungeondelvers.game.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class LootRollerTest {

    /** loot_goblin_normal from V002 seed. */
    private static final List<LootRoller.Entry> GOBLIN_NORMAL = List.of(
            new LootRoller.Entry("mat_goblin_ear", 400, 1, 2, "COMMON"),
            new LootRoller.Entry("sword_wooden", 50, 1, 1, "COMMON"),
            new LootRoller.Entry("helm_leather", 50, 1, 1, "COMMON"),
            new LootRoller.Entry("sword_iron", 20, 1, 1, "RARE"));

    @Test
    void dropRatesFollowWeights() {
        var random = new SplittableRandom(7);
        Map<String, Integer> counts = new HashMap<>();
        int rolls = 52_000;
        for (int i = 0; i < rolls; i++) {
            var drop = LootRoller.roll(GOBLIN_NORMAL, random).orElseThrow();
            counts.merge(drop.itemDefId(), 1, Integer::sum);
            if (drop.itemDefId().equals("mat_goblin_ear")) {
                assertThat(drop.quantity()).isBetween(1, 2);
            } else {
                assertThat(drop.quantity()).isEqualTo(1);
            }
        }
        assertThat(counts.get("mat_goblin_ear") / (double) rolls).isCloseTo(400 / 520.0, within(0.01));
        assertThat(counts.get("sword_iron") / (double) rolls).isCloseTo(20 / 520.0, within(0.005));
    }

    @Test
    void rarityComesFromEntry() {
        var onlyIron = List.of(new LootRoller.Entry("sword_iron", 1, 1, 1, "RARE"));
        assertThat(LootRoller.roll(onlyIron, new SplittableRandom(1)).orElseThrow().rarity()).isEqualTo("RARE");
    }

    @Test
    void emptyTableDropsNothing() {
        assertThat(LootRoller.roll(List.of(), new SplittableRandom(1))).isEmpty();
    }
}
