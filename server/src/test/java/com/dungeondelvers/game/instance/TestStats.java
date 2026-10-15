package com.dungeondelvers.game.instance;

/** Stats used across instance tests. */
final class TestStats {

    /** CharacterBaseStats WARRIOR. */
    static final CombatStats WARRIOR = new CombatStats(1000, 100, 50, 30, 0.05, 1.5, 5.0, 1.0);

    /** Same as WARRIOR but never crits, for exact damage assertions. */
    static final CombatStats WARRIOR_NO_CRIT = new CombatStats(1000, 100, 50, 30, 0.0, 1.5, 5.0, 1.0);

    private TestStats() {}
}
