package com.dungeondelvers.game.economy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decides where a drop goes in a character's bag (GDD §6.3: 40 slots, materials stack to 999).
 * Stackable items top up existing stacks of the same item + rarity first, then open new slots.
 * All-or-nothing: if the whole quantity doesn't fit, the drop is rejected (inventory full).
 */
public final class InventoryPlanner {

    public static final int CAPACITY = 40;

    /** An item already in the bag. */
    public record Slot(UUID id, int slotIndex, String itemDefId, String rarity, int quantity) {}

    public record ItemRules(boolean stackable, int maxStack) {}

    /** Add {@code added} to the existing stack {@code id}, giving {@code newQuantity}. */
    public record StackOp(UUID id, int added, int newQuantity) {}

    /** Create a new item in {@code slotIndex}. */
    public record NewSlot(int slotIndex, int quantity) {}

    public record Plan(List<StackOp> stacks, List<NewSlot> newSlots) {}

    private InventoryPlanner() {}

    /** Returns the placement plan, or empty if the bag can't hold the whole quantity. */
    public static Optional<Plan> plan(List<Slot> bag, ItemRules rules, String itemDefId, String rarity,
                                      int quantity, int capacity) {
        int remaining = quantity;
        int maxStack = rules.stackable() ? Math.max(1, rules.maxStack()) : 1;

        List<StackOp> stacks = new ArrayList<>();
        if (rules.stackable()) {
            for (Slot slot : bag) {
                if (remaining == 0) {
                    break;
                }
                if (slot.itemDefId().equals(itemDefId) && slot.rarity().equals(rarity) && slot.quantity() < maxStack) {
                    int added = Math.min(remaining, maxStack - slot.quantity());
                    stacks.add(new StackOp(slot.id(), added, slot.quantity() + added));
                    remaining -= added;
                }
            }
        }

        Set<Integer> used = new HashSet<>();
        bag.forEach(s -> used.add(s.slotIndex()));
        List<NewSlot> newSlots = new ArrayList<>();
        for (int index = 0; index < capacity && remaining > 0; index++) {
            if (!used.contains(index)) {
                int quantityHere = Math.min(remaining, maxStack);
                newSlots.add(new NewSlot(index, quantityHere));
                remaining -= quantityHere;
            }
        }
        return remaining == 0 ? Optional.of(new Plan(stacks, newSlots)) : Optional.empty();
    }
}
