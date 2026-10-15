package com.dungeondelvers.game.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dungeondelvers.game.session.GameSession;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerVitalsTest {

    private final GameSession session = session();

    @Test
    void savedVitalsAreRestored() {
        var player = new PlayerEntity(session, "P", 1, "WARRIOR", TestStats.WARRIOR, 640, 30, Vec3.ZERO, 0);

        assertThat(player.getHp()).isEqualTo(640);
        assertThat(player.getMp()).isEqualTo(30);
    }

    @Test
    void nothingSavedMeansFull() {
        var player = new PlayerEntity(session, "P", 1, "WARRIOR", TestStats.WARRIOR, null, null, Vec3.ZERO, 0);

        assertThat(player.getHp()).isEqualTo(1000);
        assertThat(player.getMp()).isEqualTo(100);
    }

    @Test
    void leftWhileDeadComesBackFull() {
        var player = new PlayerEntity(session, "P", 1, "WARRIOR", TestStats.WARRIOR, 0, 0, Vec3.ZERO, 0);

        assertThat(player.isAlive()).isTrue();
        assertThat(player.getHp()).isEqualTo(1000);
    }

    @Test
    void savedValuesAreCappedToMax() {
        var player = new PlayerEntity(session, "P", 1, "WARRIOR", TestStats.WARRIOR, 5000, 500, Vec3.ZERO, 0);

        assertThat(player.getHp()).isEqualTo(1000);
        assertThat(player.getMp()).isEqualTo(100);
    }

    private static GameSession session() {
        GameSession s = mock(GameSession.class);
        when(s.getCharacterId()).thenReturn(UUID.randomUUID());
        return s;
    }
}
