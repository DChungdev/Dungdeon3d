package com.dungeondelvers.game.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dungeondelvers.game.session.GameSession;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerEntityTest {

    private static final double HALF_EXTENT = 20.0;
    private static final Vec3 MOVING = new Vec3(0, 0, 1);

    private PlayerEntity player;
    private long t0;

    @BeforeEach
    void setUp() {
        GameSession session = mock(GameSession.class);
        when(session.getCharacterId()).thenReturn(UUID.randomUUID());
        t0 = 1_000_000;
        // moveSpeed 5 m/s → 0.5 m per 100 ms, 0.55 m with tolerance
        player = new PlayerEntity(session, "DelverOne", 1, "WARRIOR", TestStats.WARRIOR, Vec3.ZERO, t0);
    }

    @Test
    void acceptsMoveWithinSpeed() {
        var result = player.applyMove(new Vec3(0, 0, 0.5), 90, MOVING, 1, HALF_EXTENT, t0 + 100);

        assertThat(result).isEqualTo(PlayerEntity.MoveResult.ACCEPTED);
        assertThat(player.getPosition().z()).isEqualTo(0.5);
        assertThat(player.getState()).isEqualTo(PlayerEntity.State.MOVING);
        assertThat(player.getLastInputSeq()).isEqualTo(1);
    }

    @Test
    void correctsSpeedHackToMaxReachableDistance() {
        var result = player.applyMove(new Vec3(0, 0, 10), 0, MOVING, 1, HALF_EXTENT, t0 + 100);

        assertThat(result).isEqualTo(PlayerEntity.MoveResult.CORRECTED);
        // 5 * 1.1 * 0.1 + 0.1 epsilon
        assertThat(player.getPosition().z()).isCloseTo(0.65, within(1e-9));
    }

    @Test
    void acceptsPacketsBunchedByNetworkJitter() {
        // Client sent at 50 ms intervals, but the 2nd and 3rd arrive together.
        assertThat(player.applyMove(new Vec3(0, 0, 0.25), 0, MOVING, 1, HALF_EXTENT, t0 + 50))
                .isEqualTo(PlayerEntity.MoveResult.ACCEPTED);
        assertThat(player.applyMove(new Vec3(0, 0, 0.50), 0, MOVING, 2, HALF_EXTENT, t0 + 150))
                .isEqualTo(PlayerEntity.MoveResult.ACCEPTED);
        assertThat(player.applyMove(new Vec3(0, 0, 0.75), 0, MOVING, 3, HALF_EXTENT, t0 + 151))
                .isEqualTo(PlayerEntity.MoveResult.ACCEPTED);
    }

    @Test
    void sustainedSpeedHackIsCapped() {
        // Client claims 10 m/s for 3 s; server allows ~5.5 m/s.
        for (int i = 1; i <= 60; i++) {
            player.applyMove(new Vec3(0, 0, i * 0.5), 0, MOVING, i, HALF_EXTENT, t0 + i * 50L);
        }
        // allowed: 5.5 m/s * 3 s + one-off 0.1 m tolerance
        assertThat(player.getPosition().z()).isLessThanOrEqualTo(5.5 * 3 + 0.1 + 1e-9);
    }

    @Test
    void capsBankedTimeSoPausesCannotTeleport() {
        // 10 s without sending → still only MAX_MOVE_DT_SEC (0.5 s) of travel allowed
        player.applyMove(new Vec3(50, 0, 0), 0, MOVING, 1, HALF_EXTENT, t0 + 10_000);

        assertThat(player.getPosition().x()).isCloseTo(5.0 * 1.1 * 0.5 + 0.1, within(1e-9));
    }

    @Test
    void clampsToRoomBounds() {
        for (int i = 1; i <= 60; i++) {
            player.applyMove(new Vec3(100, 0, 0), 0, MOVING, i, HALF_EXTENT, t0 + i * 500L);
        }
        assertThat(player.getPosition().x()).isEqualTo(HALF_EXTENT);
    }

    @Test
    void dropsIntentsAboveTwentyPerSecond() {
        for (int i = 1; i <= 20; i++) {
            assertThat(player.applyMove(Vec3.ZERO, 0, null, i, HALF_EXTENT, t0 + i))
                    .isNotEqualTo(PlayerEntity.MoveResult.RATE_LIMITED);
        }
        assertThat(player.applyMove(Vec3.ZERO, 0, null, 21, HALF_EXTENT, t0 + 21))
                .isEqualTo(PlayerEntity.MoveResult.RATE_LIMITED);
        assertThat(player.getLastInputSeq()).isEqualTo(20);
        // next window
        assertThat(player.applyMove(Vec3.ZERO, 0, null, 22, HALF_EXTENT, t0 + 1001))
                .isNotEqualTo(PlayerEntity.MoveResult.RATE_LIMITED);
    }

    @Test
    void becomesIdleWhenIntentsStop() {
        player.applyMove(new Vec3(0, 0, 0.5), 0, MOVING, 1, HALF_EXTENT, t0 + 100);
        player.consumeDirty();

        player.tick(t0 + 200, 0.1);
        assertThat(player.getState()).isEqualTo(PlayerEntity.State.MOVING);

        player.tick(t0 + 100 + PlayerEntity.MOVE_TIMEOUT_MS + 1, 0.1);
        assertThat(player.getState()).isEqualTo(PlayerEntity.State.IDLE);
        assertThat(player.consumeDirty()).isTrue();
    }

    @Test
    void normalizesRotation() {
        player.applyMove(Vec3.ZERO, -90, null, 1, HALF_EXTENT, t0 + 50);
        assertThat(player.delta().rotation()).isEqualTo(270);
    }
}
