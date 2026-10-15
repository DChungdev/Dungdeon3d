package com.dungeondelvers.game.instance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class DamageCalculatorTest {

    private static final RandomGenerator NEVER_CRIT = () -> -1L; // nextDouble() → ~1.0
    private static final RandomGenerator ALWAYS_CRIT = () -> 0L;           // nextDouble() → 0.0

    private static final CombatStats WARRIOR = TestStats.WARRIOR;
    private static final CombatStats GRUNT = new CombatStats(150, 0, 8, 3, 0, 1.5, 3.5, 1.0);

    @Test
    void autoAttackFollowsTddFormula() {
        // raw = 0 + 50 * 1.0 = 50; mitigated = 50 * 100 / 103 = 48.54 → 48
        var hit = DamageCalculator.calculate(0, 1.0, WARRIOR, GRUNT, NEVER_CRIT);
        assertThat(hit.amount()).isEqualTo(48);
        assertThat(hit.crit()).isFalse();
    }

    @Test
    void skillAddsBaseDamageAndScaling() {
        // Charge: raw = 20 + 50 * 0.8 = 60; 60 * 100 / 103 = 58.25 → 58
        assertThat(DamageCalculator.calculate(20, 0.8, WARRIOR, GRUNT, NEVER_CRIT).amount()).isEqualTo(58);
    }

    @Test
    void critMultipliesMitigatedDamage() {
        // 48.54 * 1.5 = 72.8 → 72
        var hit = DamageCalculator.calculate(0, 1.0, WARRIOR, GRUNT, ALWAYS_CRIT);
        assertThat(hit.crit()).isTrue();
        assertThat(hit.amount()).isEqualTo(72);
    }

    @Test
    void mobHitOnWarrior() {
        // 8 * 100 / 130 = 6.15 → 6
        assertThat(DamageCalculator.calculate(0, 1.0, GRUNT, WARRIOR, NEVER_CRIT).amount()).isEqualTo(6);
    }

    @Test
    void minimumDamageIsOne() {
        var weak = new CombatStats(10, 0, 0, 0, 0, 1.5, 1, 1);
        var tank = new CombatStats(10, 0, 0, 10_000, 0, 1.5, 1, 1);
        assertThat(DamageCalculator.calculate(0, 1.0, weak, tank, NEVER_CRIT).amount()).isEqualTo(1);
    }
}
