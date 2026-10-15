package com.dungeondelvers.game.instance;

import java.util.random.RandomGenerator;

/**
 * TDD §8.2:
 * <pre>
 * rawDamage   = skillBaseDamage + ATK * skillScaling
 * mitigated   = rawDamage * (100 / (100 + DEF))
 * if crit:      mitigated *= critDamage
 * finalDamage = max(1, floor(mitigated))
 * </pre>
 */
public final class DamageCalculator {

    public record Hit(int amount, boolean crit) {}

    private DamageCalculator() {}

    public static Hit calculate(int baseDamage, double atkScaling, CombatStats attacker, CombatStats defender,
                                RandomGenerator random) {
        double raw = baseDamage + attacker.atk() * atkScaling;
        double mitigated = raw * (100.0 / (100.0 + Math.max(0, defender.def())));
        boolean crit = random.nextDouble() < attacker.critRate();
        if (crit) {
            mitigated *= attacker.critDamage();
        }
        return new Hit(Math.max(1, (int) Math.floor(mitigated)), crit);
    }
}
