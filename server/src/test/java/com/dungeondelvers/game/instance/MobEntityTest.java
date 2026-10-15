package com.dungeondelvers.game.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dungeondelvers.game.session.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MobEntityTest {

    private static final CombatCatalog.MobDef GRUNT = new CombatCatalog.MobDef("mob_goblin_grunt", "Goblin Grunt", 1,
            new CombatStats(150, 0, 8, 3, 0, 1.5, 3.5, 1.0), 8.0, 20.0, 2.0, "loot_goblin_normal");
    private static final double DT = 0.1;

    private final List<CombatEntity> hitsOnPlayers = new ArrayList<>();
    private final MobEntity.Context context =
            (source, target, base, scaling, skillId, now) -> hitsOnPlayers.add(target);
    private MobEntity mob;
    private long now;

    @BeforeEach
    void setUp() {
        mob = new MobEntity(GRUNT, Vec3.ZERO);
        now = 1_000_000;
    }

    @Test
    void staysIdleWhenNoPlayerInAggroRadius() {
        PlayerEntity far = player(new Vec3(0, 0, 9));

        mob.updateAi(List.of(far), DT, now, context);

        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.IDLE);
    }

    @Test
    void chasesThenAttacksPlayerInRange() {
        PlayerEntity player = player(new Vec3(0, 0, 6));

        mob.updateAi(List.of(player), DT, now, context);
        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.CHASE);

        // 3.5 m/s: one tick moves 0.35 m towards the player
        mob.updateAi(List.of(player), DT, now += 100, context);
        assertThat(mob.getPosition().z()).isCloseTo(0.35, within(1e-9));

        runTicks(List.of(player), 20);
        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.ATTACK);
        assertThat(mob.getPosition().distanceXz(player.getPosition())).isLessThanOrEqualTo(GRUNT.attackRangeM());

        hitsOnPlayers.clear();
        runTicks(List.of(player), 30); // 3 s at 1 attack/s
        assertThat(hitsOnPlayers).hasSize(3).containsOnly(player);
    }

    @Test
    void returnsHomeAndHealsWhenLeashed() {
        PlayerEntity player = player(new Vec3(0, 0, 5));
        mob.updateAi(List.of(player), DT, now, context);
        mob.takeDamage(100);

        // Player runs away faster than the mob; the mob follows until 20 m from spawn.
        for (int i = 0; i < 200 && mob.getAiState() != MobEntity.AiState.RETURN; i++) {
            player.applyMove(new Vec3(0, 0, 5 + i * 0.5), 0, new Vec3(0, 0, 5), i + 1, 100, now);
            mob.updateAi(List.of(player), DT, now += 100, context);
        }
        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.RETURN);

        runTicks(List.of(player), 200);
        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.IDLE);
        assertThat(mob.getPosition()).isEqualTo(Vec3.ZERO);
        assertThat(mob.getHp()).isEqualTo(150);
    }

    @Test
    void fightsBackWhenHitFromOutsideAggroRadius() {
        PlayerEntity sniper = player(new Vec3(0, 0, 10));

        mob.onAttackedBy(sniper);

        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.CHASE);
        assertThat(mob.getTarget()).isSameAs(sniper);
    }

    @Test
    void switchesTargetWhenCurrentTargetDies() {
        PlayerEntity first = player(new Vec3(0, 0, 1.5));
        PlayerEntity second = player(new Vec3(2.0, 0, 0));
        mob.updateAi(List.of(first, second), DT, now, context);
        assertThat(mob.getTarget()).isSameAs(first);

        first.takeDamage(10_000);
        first.onDeath(now);
        runTicks(List.of(first, second), 5);

        assertThat(mob.getTarget()).isSameAs(second);
    }

    @Test
    void deadMobDoesNothing() {
        PlayerEntity player = player(new Vec3(0, 0, 1));
        mob.takeDamage(1000);
        mob.onDeath(now);

        runTicks(List.of(player), 20);

        assertThat(mob.getAiState()).isEqualTo(MobEntity.AiState.DEAD);
        assertThat(hitsOnPlayers).isEmpty();
    }

    private void runTicks(List<PlayerEntity> players, int ticks) {
        for (int i = 0; i < ticks; i++) {
            mob.updateAi(players, DT, now += 100, context);
        }
    }

    private PlayerEntity player(Vec3 position) {
        GameSession session = mock(GameSession.class);
        when(session.getCharacterId()).thenReturn(UUID.randomUUID());
        return new PlayerEntity(session, "P", 1, "WARRIOR", TestStats.WARRIOR, position, now);
    }
}
