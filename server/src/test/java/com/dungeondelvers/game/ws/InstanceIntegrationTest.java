package com.dungeondelvers.game.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dungeondelvers.auth.jwt.JwtService;
import com.dungeondelvers.game.CharacterBaseStats;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.repository.AccountRepository;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InstanceIntegrationTest {

    private static final String JOIN_GOBLIN_CAVE = "{\"dungeonId\":\"dungeon_goblin_cave\",\"difficulty\":\"NORMAL\"}";

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AccountRepository accountRepository;

    @MockitoBean
    CharacterRepository characterRepository;

    private final List<WsTestClient> clients = new ArrayList<>();

    @AfterEach
    void leaveAndClose() throws Exception {
        // Rooms live in the shared application context; leave explicitly so the next
        // test doesn't find stale players (a plain disconnect keeps them for the grace period).
        for (WsTestClient client : clients) {
            if (client.isOpen()) {
                client.send("LEAVE_INSTANCE", "{}");
                client.close();
            }
        }
    }

    @Test
    void twoPlayersInSameInstanceSeeEachOther() throws Exception {
        Player a = newPlayer("Alpha");
        Player b = newPlayer("Bravo");

        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        JsonNode enterA = a.client.next("INSTANCE_ENTER");
        assertThat(enterA.get("localPlayerId").asString()).isEqualTo(a.characterId.toString());
        assertThat(enterA.get("roomTemplateIds")).hasSize(3);
        assertThat(entityIds(enterA.get("entities"))).containsExactly(a.characterId.toString());

        b.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        JsonNode enterB = b.client.next("INSTANCE_ENTER");
        assertThat(enterB.get("instanceId").asString()).isEqualTo(enterA.get("instanceId").asString());
        assertThat(entityIds(enterB.get("entities")))
                .containsExactlyInAnyOrder(a.characterId.toString(), b.characterId.toString());

        JsonNode spawn = a.client.next("ENTITY_SPAWN").get("entity");
        assertThat(spawn.get("entityId").asString()).isEqualTo(b.characterId.toString());
        assertThat(spawn.get("entityType").asString()).isEqualTo("PLAYER");
        assertThat(spawn.get("name").asString()).isEqualTo("Bravo");
        assertThat(spawn.get("maxHp").asInt()).isEqualTo(1000);
    }

    @Test
    void movementIsBroadcastToRoomWithInputAck() throws Exception {
        Player a = newPlayer("Alpha");
        Player b = newPlayer("Bravo");
        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        Vec spawnA = position(a.client.next("INSTANCE_ENTER").get("entities"), a.characterId);
        b.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        b.client.next("INSTANCE_ENTER");

        Thread.sleep(150);
        a.client.send("MOVE_INTENT", moveIntent(spawnA.x, spawnA.z + 0.5, 42));

        JsonNode seenByB = findEntity(b.client.next("ENTITY_UPDATE").get("entities"), a.characterId);
        assertThat(seenByB.get("position").get("z").asDouble()).isEqualTo(spawnA.z + 0.5);
        assertThat(seenByB.get("state").asString()).isEqualTo("MOVING");
        JsonNode seenByA = findEntity(a.client.next("ENTITY_UPDATE").get("entities"), a.characterId);
        assertThat(seenByA.get("lastInputSeq").asLong()).isEqualTo(42);
    }

    @Test
    void speedHackIsCorrectedByServer() throws Exception {
        Player a = newPlayer("Alpha");
        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        Vec spawn = position(a.client.next("INSTANCE_ENTER").get("entities"), a.characterId);

        a.client.send("MOVE_INTENT", moveIntent(spawn.x, spawn.z + 15, 1));

        JsonNode self = findEntity(a.client.next("ENTITY_UPDATE").get("entities"), a.characterId);
        double movedZ = self.get("position").get("z").asDouble() - spawn.z;
        assertThat(movedZ).isPositive().isLessThan(1.0);
    }

    @Test
    void leavingDespawnsForOthers() throws Exception {
        Player a = newPlayer("Alpha");
        Player b = newPlayer("Bravo");
        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        a.client.next("INSTANCE_ENTER");
        b.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        b.client.next("INSTANCE_ENTER");

        a.client.send("LEAVE_INSTANCE", "{}");

        assertThat(b.client.next("ENTITY_DESPAWN").get("entityId").asString())
                .isEqualTo(a.characterId.toString());
        a.client.send("MOVE_INTENT", moveIntent(0, 0, 1));
        assertThat(a.client.next("ERROR").get("code").asString()).isEqualTo("NOT_IN_ZONE");
    }

    @Test
    void moveBeforeJoinIsRejected() throws Exception {
        Player a = newPlayer("Alpha");

        a.client.send("MOVE_INTENT", moveIntent(1, 1, 1));

        assertThat(a.client.next("ERROR").get("code").asString()).isEqualTo("NOT_IN_ZONE");
    }

    @Test
    void invalidMovePayloadIsRejected() throws Exception {
        Player a = newPlayer("Alpha");
        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        a.client.next("INSTANCE_ENTER");

        a.client.send("MOVE_INTENT", "{\"position\":{\"x\":\"far\",\"z\":0},\"rotation\":0,\"inputSeq\":1}");

        assertThat(a.client.next("ERROR").get("code").asString()).isEqualTo("INVALID_MESSAGE");
    }

    @Test
    void unknownDungeonIsRejected() throws Exception {
        Player a = newPlayer("Alpha");

        a.client.send("JOIN_INSTANCE", "{\"dungeonId\":\"dungeon_nope\"}");

        assertThat(a.client.next("ERROR").get("code").asString()).isEqualTo("INSTANCE_NOT_FOUND");
    }

    @Test
    void reconnectResendsInstanceSnapshot() throws Exception {
        Player a = newPlayer("Alpha");
        a.client.send("JOIN_INSTANCE", JOIN_GOBLIN_CAVE);
        String instanceId = a.client.next("INSTANCE_ENTER").get("instanceId").asString();
        a.client.close();

        WsTestClient again = connect(a.token);
        again.next("CONNECTED");
        again.send("RECONNECT", "{\"zoneType\":\"INSTANCE\",\"zoneId\":\"" + instanceId + "\",\"lastSeq\":0}");

        JsonNode enter = again.next("INSTANCE_ENTER");
        assertThat(enter.get("instanceId").asString()).isEqualTo(instanceId);
        assertThat(entityIds(enter.get("entities"))).contains(a.characterId.toString());
    }

    // --- helpers ---

    private record Player(UUID characterId, String token, WsTestClient client) {}

    private record Vec(double x, double z) {}

    private Player newPlayer(String name) throws Exception {
        UUID accountId = UUID.randomUUID();
        CharacterEntity character = new CharacterEntity();
        character.setId(UUID.randomUUID());
        character.setAccountId(accountId);
        character.setName(name);
        character.setClassType("WARRIOR");
        character.setStatsJson(CharacterBaseStats.jsonForClass("WARRIOR"));
        when(characterRepository.findByAccountIdOrderByCreatedAtAsc(accountId)).thenReturn(List.of(character));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));

        String token = jwtService.createAccessToken(accountId);
        WsTestClient client = connect(token);
        client.next("CONNECTED");
        return new Player(character.getId(), token, client);
    }

    private WsTestClient connect(String token) throws Exception {
        WsTestClient client = WsTestClient.connect(port, token, objectMapper);
        clients.add(client);
        return client;
    }

    private static String moveIntent(double x, double z, long inputSeq) {
        return "{\"position\":{\"x\":%s,\"y\":0,\"z\":%s},\"velocity\":{\"x\":0,\"y\":0,\"z\":1},\"rotation\":0,\"inputSeq\":%d}"
                .formatted(x, z, inputSeq);
    }

    private static List<String> entityIds(JsonNode entities) {
        List<String> ids = new ArrayList<>();
        entities.forEach(e -> ids.add(e.get("entityId").asString()));
        return ids;
    }

    private static JsonNode findEntity(JsonNode entities, UUID entityId) {
        for (JsonNode e : entities) {
            if (e.get("entityId").asString().equals(entityId.toString())) {
                return e;
            }
        }
        throw new AssertionError("entity " + entityId + " not in " + entities);
    }

    private static Vec position(JsonNode entities, UUID entityId) {
        JsonNode p = findEntity(entities, entityId).get("position");
        return new Vec(p.get("x").asDouble(), p.get("z").asDouble());
    }
}
