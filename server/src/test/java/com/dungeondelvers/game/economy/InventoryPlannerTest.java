package com.dungeondelvers.game.economy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryPlannerTest {

    private static final InventoryPlanner.ItemRules MATERIAL = new InventoryPlanner.ItemRules(true, 999);
    private static final InventoryPlanner.ItemRules WEAPON = new InventoryPlanner.ItemRules(false, 1);

    @Test
    void emptyBagUsesFirstSlot() {
        var plan = InventoryPlanner.plan(List.of(), WEAPON, "sword_wooden", "COMMON", 1, 40).orElseThrow();

        assertThat(plan.stacks()).isEmpty();
        assertThat(plan.newSlots()).containsExactly(new InventoryPlanner.NewSlot(0, 1));
    }

    @Test
    void stackableTopsUpMatchingStack() {
        UUID ears = UUID.randomUUID();
        var bag = List.of(
                slot(ears, 0, "mat_goblin_ear", "COMMON", 5),
                slot(UUID.randomUUID(), 1, "sword_wooden", "COMMON", 1));

        var plan = InventoryPlanner.plan(bag, MATERIAL, "mat_goblin_ear", "COMMON", 2, 40).orElseThrow();

        assertThat(plan.stacks()).containsExactly(new InventoryPlanner.StackOp(ears, 2, 7));
        assertThat(plan.newSlots()).isEmpty();
    }

    @Test
    void differentRarityDoesNotStack() {
        var bag = List.of(slot(UUID.randomUUID(), 0, "mat_goblin_ear", "COMMON", 5));

        var plan = InventoryPlanner.plan(bag, MATERIAL, "mat_goblin_ear", "RARE", 1, 40).orElseThrow();

        assertThat(plan.stacks()).isEmpty();
        assertThat(plan.newSlots()).containsExactly(new InventoryPlanner.NewSlot(1, 1));
    }

    @Test
    void overflowingStackSpillsIntoNewSlot() {
        UUID ears = UUID.randomUUID();
        var bag = List.of(slot(ears, 0, "mat_goblin_ear", "COMMON", 998));

        var plan = InventoryPlanner.plan(bag, MATERIAL, "mat_goblin_ear", "COMMON", 3, 40).orElseThrow();

        assertThat(plan.stacks()).containsExactly(new InventoryPlanner.StackOp(ears, 1, 999));
        assertThat(plan.newSlots()).containsExactly(new InventoryPlanner.NewSlot(1, 2));
    }

    @Test
    void fillsGapsBeforeLaterSlots() {
        var bag = List.of(
                slot(UUID.randomUUID(), 0, "sword_wooden", "COMMON", 1),
                slot(UUID.randomUUID(), 2, "helm_leather", "COMMON", 1));

        var plan = InventoryPlanner.plan(bag, WEAPON, "sword_wooden", "COMMON", 1, 40).orElseThrow();

        assertThat(plan.newSlots()).containsExactly(new InventoryPlanner.NewSlot(1, 1));
    }

    @Test
    void fullBagRejectsNonStackable() {
        assertThat(InventoryPlanner.plan(fullBag(), WEAPON, "sword_wooden", "COMMON", 1, 40)).isEmpty();
    }

    @Test
    void fullBagStillAcceptsStackableIntoExistingStack() {
        var bag = new ArrayList<>(fullBag());
        UUID ears = UUID.randomUUID();
        bag.set(39, slot(ears, 39, "mat_goblin_ear", "COMMON", 10));

        var plan = InventoryPlanner.plan(bag, MATERIAL, "mat_goblin_ear", "COMMON", 2, 40).orElseThrow();

        assertThat(plan.stacks()).containsExactly(new InventoryPlanner.StackOp(ears, 2, 12));
    }

    @Test
    void partialFitIsRejectedEntirely() {
        var bag = new ArrayList<>(fullBag());
        bag.set(39, slot(UUID.randomUUID(), 39, "mat_goblin_ear", "COMMON", 998));

        assertThat(InventoryPlanner.plan(bag, MATERIAL, "mat_goblin_ear", "COMMON", 2, 40)).isEmpty();
    }

    private static List<InventoryPlanner.Slot> fullBag() {
        List<InventoryPlanner.Slot> bag = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            bag.add(slot(UUID.randomUUID(), i, "helm_leather", "COMMON", 1));
        }
        return bag;
    }

    private static InventoryPlanner.Slot slot(UUID id, int index, String def, String rarity, int qty) {
        return new InventoryPlanner.Slot(id, index, def, rarity, qty);
    }
}
