package com.dungeondelvers.game.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dungeondelvers.game.session.GameSession;
import com.dungeondelvers.game.ws.MessageSender;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InstanceRoomCombatTest {

    private static final DungeonCatalog.DungeonDef GOBLIN_CAVE = new DungeonCatalog.DungeonDef(
            "dungeon_goblin_cave", 4, List.of("goblin_room_1", "goblin_room_2", "goblin_boss"));
    private static final RandomGenerator NEVER_CRIT = () -> -1L;

    private final MessageSender sender = mock(MessageSender.class);
    private final GameSession session = mock(GameSession.class);
    private final List<MobKillListener.MobKill> kills = new ArrayList<>();
    private InstanceRoom room;
    private PlayerEntity player;
    private long now;

    @BeforeEach
    void setUp() {
        now = 1_000_000;
        when(session.getCharacterId()).thenReturn(UUID.randomUUID());
        when(session.getState()).thenReturn(GameSession.ConnectionState.CONNECTED);
        room = new InstanceRoom(GOBLIN_CAVE, "NORMAL", 42, 4, 100, sender, NEVER_CRIT, kills::add, now);
        player = new PlayerEntity(session, "Hero", 1, "WARRIOR", TestStats.WARRIOR_NO_CRIT, new Vec3(-2, 0, 0), now);
        room.addPlayer(player);
    }

    @Test
    void roomStartsWithThreeGoblins() {
        assertThat(room.mobs()).extracting(m -> m.getDef().id())
                .containsExactly("mob_goblin_grunt", "mob_goblin_grunt", "mob_goblin_archer");
    }

    @Test
    void autoAttackKillsMobWhichDespawnsAndRespawns() {
        MobEntity grunt = room.mobs().get(0);
        walkTo(grunt.getPosition(), 1.5);
        assertThat(room.setAutoAttackTarget(player.getEntityId(), grunt.getEntityId())).isTrue();

        tickFor(5_000); // 48 dmg/s vs 150 HP → dead after 4 swings
        assertThat(grunt.isAlive()).isFalse();
        assertThat(player.getAutoAttackTargetId()).isNull();
        var death = ArgumentCaptor.forClass(Object.class);
        verify(sender, atLeastOnce()).send(eq(session), eq("ENTITY_DEATH"), death.capture());
        assertThat(death.getAllValues())
                .contains(new InstancePayloads.EntityDeath(grunt.getEntityId(), player.getEntityId()));

        tickFor(InstanceRoom.MOB_DESPAWN_DELAY_MS);
        assertThat(room.mobs()).doesNotContain(grunt).hasSize(2);
        verify(sender).send(session, "ENTITY_DESPAWN", new InstancePayloads.EntityDespawn(grunt.getEntityId()));

        tickFor(InstanceRoom.MOB_RESPAWN_DELAY_MS);
        assertThat(room.mobs()).hasSize(3);
    }

    @Test
    void killRewardsOnlyPlayersWhoDamagedTheMob() {
        GameSession helperSession = mock(GameSession.class);
        when(helperSession.getCharacterId()).thenReturn(UUID.randomUUID());
        when(helperSession.getState()).thenReturn(GameSession.ConnectionState.CONNECTED);
        PlayerEntity bystander = new PlayerEntity(helperSession, "Bystander", 1, "WARRIOR",
                TestStats.WARRIOR_NO_CRIT, new Vec3(2, 0, 0), now);
        room.addPlayer(bystander);

        MobEntity grunt = room.mobs().get(0);
        walkTo(grunt.getPosition(), 1.5);
        room.setAutoAttackTarget(player.getEntityId(), grunt.getEntityId());
        tickFor(5_000);

        assertThat(kills).hasSize(1);
        var kill = kills.get(0);
        assertThat(kill.mobEntityId()).isEqualTo(grunt.getEntityId());
        assertThat(kill.mobDefId()).isEqualTo("mob_goblin_grunt");
        assertThat(kill.lootTableId()).isEqualTo("loot_goblin_normal");
        assertThat(kill.rewarded()).containsExactly(session);
    }

    @Test
    void removedPlayerKeepsHpForSaving() {
        player.takeDamage(123);

        var removed = room.removePlayer(player.getEntityId()).orElseThrow();

        assertThat(removed.getHp()).isEqualTo(1000 - 123);
        assertThat(room.removePlayer(player.getEntityId())).isEmpty();
    }

    @Test
    void autoAttackDoesNothingOutOfRange() {
        MobEntity archer = room.mobs().get(2); // 16 m away, aggro 10 m
        room.setAutoAttackTarget(player.getEntityId(), archer.getEntityId());

        tickFor(3_000);

        assertThat(archer.getHp()).isEqualTo(archer.getStats().maxHp());
    }

    @Test
    void chargeHitsSpendsMpAndStartsCooldown() {
        MobEntity grunt = room.mobs().get(0);
        walkTo(grunt.getPosition(), 7.0);

        var result = room.castSkill(player.getEntityId(), "warrior_charge", grunt.getEntityId(), now).orElseThrow();

        assertThat(result.success()).isTrue();
        assertThat(result.cooldownRemainingMs()).isEqualTo(8_000);
        assertThat(result.cooldownEndTick()).isEqualTo(room.currentTick() + 80);
        assertThat(result.mpRemaining()).isEqualTo(85);
        assertThat(grunt.getHp()).isEqualTo(150 - 58);

        var again = room.castSkill(player.getEntityId(), "warrior_charge", grunt.getEntityId(), now + 1000)
                .orElseThrow();
        assertThat(again.success()).isFalse();
        assertThat(again.reason()).isEqualTo("SKILL_ON_COOLDOWN");
        assertThat(again.cooldownRemainingMs()).isEqualTo(7_000);
    }

    @Test
    void chargeOutOfRangeIsRejected() {
        MobEntity archer = room.mobs().get(2); // 16 m, Charge range 8 m

        var result = room.castSkill(player.getEntityId(), "warrior_charge", archer.getEntityId(), now).orElseThrow();

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("TARGET_INVALID");
        assertThat(result.mpRemaining()).isEqualTo(100);
        assertThat(archer.getHp()).isEqualTo(120);
    }

    @Test
    void unknownSkillIsRejected() {
        var result = room.castSkill(player.getEntityId(), "mage_firebolt", null, now).orElseThrow();

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("UNKNOWN_SKILL");
    }

    @Test
    void notEnoughMpIsRejected() {
        MobEntity grunt = room.mobs().get(0);
        walkTo(grunt.getPosition(), 7.0);
        player.trySpendMp(90);

        var result = room.castSkill(player.getEntityId(), "warrior_charge", grunt.getEntityId(), now).orElseThrow();

        assertThat(result.reason()).isEqualTo("INSUFFICIENT_MP");
    }

    @Test
    void skillSpamIsRateLimited() {
        int answered = 0;
        for (int i = 0; i < 10; i++) {
            if (room.castSkill(player.getEntityId(), "warrior_charge", null, now).isPresent()) {
                answered++;
            }
        }
        assertThat(answered).isEqualTo(PlayerEntity.MAX_SKILL_CASTS_PER_SEC);
    }

    @Test
    void mobsAggroAndDamagePlayerWhoWalksUp() {
        MobEntity grunt = room.mobs().get(0);
        walkTo(grunt.getPosition(), 3.0);

        tickFor(4_000);

        assertThat(grunt.getAiState()).isEqualTo(MobEntity.AiState.ATTACK);
        assertThat(player.getHp()).isLessThan(player.getStats().maxHp());
        verify(sender, atLeastOnce()).send(eq(session), eq("DAMAGE_EVENT"), any());
    }

    @Test
    void cannotTargetMissingOrDeadMob() {
        assertThat(room.setAutoAttackTarget(player.getEntityId(), UUID.randomUUID())).isFalse();
        assertThat(room.setAutoAttackTarget(player.getEntityId(), null)).isTrue();
    }

    @Test
    void deadPlayerRespawnsAtSpawnWithFullHp() {
        player.takeDamage(10_000);
        player.onDeath(now);
        assertThat(player.applyMove(new Vec3(-2, 0, 1), 0, null, 999, 20, now + 100))
                .isEqualTo(PlayerEntity.MoveResult.DEAD);

        tickFor(PlayerEntity.RESPAWN_DELAY_MS + 200);

        assertThat(player.isAlive()).isTrue();
        assertThat(player.getHp()).isEqualTo(1000);
        assertThat(player.getPosition()).isEqualTo(new Vec3(-2, 0, 0));
    }

    /** Walks the player in 0.5 m / 100 ms steps until within {@code distance} of {@code target}, ticking the room. */
    private void walkTo(Vec3 target, double distance) {
        long seq = player.getLastInputSeq();
        while (player.getPosition().distanceXz(target) > distance) {
            now += 100;
            Vec3 next = player.getPosition().moveTowardsXz(target, 0.5);
            room.applyMove(player.getEntityId(), next, 0, new Vec3(0, 0, 1), ++seq, now);
            room.tick(now);
        }
    }

    private void tickFor(long durationMs) {
        long end = now + durationMs;
        while (now < end) {
            now += 100;
            room.tick(now);
        }
    }
}
