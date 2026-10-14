package com.dungeondelvers.game.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dungeondelvers.auth.jwt.JwtService;
import com.dungeondelvers.persistence.entity.CharacterEntity;
import com.dungeondelvers.persistence.repository.AccountRepository;
import com.dungeondelvers.persistence.repository.CharacterRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameWebSocketIntegrationTest {

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

    private final UUID accountId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();
    private final StandardWebSocketClient client = new StandardWebSocketClient();
    private final List<WebSocketSession> openConnections = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CharacterEntity character = new CharacterEntity();
        character.setId(characterId);
        character.setAccountId(accountId);
        character.setName("DelverOne");
        character.setClassType("WARRIOR");
        when(characterRepository.findByAccountIdOrderByCreatedAtAsc(any())).thenReturn(List.of(character));
    }

    @AfterEach
    void tearDown() throws Exception {
        for (WebSocketSession connection : openConnections) {
            if (connection.isOpen()) {
                connection.close();
            }
        }
    }

    @Test
    void rejectsConnectionWithoutToken() {
        assertThatThrownBy(() -> connect(null)).isInstanceOf(Exception.class);
    }

    @Test
    void rejectsConnectionWithInvalidToken() {
        assertThatThrownBy(() -> connect("not-a-jwt")).isInstanceOf(Exception.class);
    }

    @Test
    void sendsConnectedOnHandshake() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));

        JsonNode connected = c.next();
        assertThat(connected.get("type").asString()).isEqualTo("CONNECTED");
        assertThat(connected.get("seq").asLong()).isEqualTo(1);
        assertThat(connected.get("payload").get("characterId").asString()).isEqualTo(characterId.toString());
        assertThat(connected.get("payload").get("configVersion").asString()).isEqualTo("1.0.0");
        assertThat(connected.get("payload").get("sessionId").asString()).isNotBlank();
    }

    @Test
    void repliesPongToPing() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));
        c.next(); // CONNECTED

        c.send("{\"type\":\"PING\",\"seq\":1,\"payload\":{}}");

        JsonNode pong = c.next();
        assertThat(pong.get("type").asString()).isEqualTo("PONG");
        assertThat(pong.get("payload").get("serverTime").asLong()).isPositive();
    }

    @Test
    void dropsDuplicateSeq() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));
        c.next(); // CONNECTED

        c.send("{\"type\":\"PING\",\"seq\":5,\"payload\":{}}");
        c.send("{\"type\":\"PING\",\"seq\":5,\"payload\":{}}");
        c.send("{\"type\":\"PING\",\"seq\":3,\"payload\":{}}");

        assertThat(c.next().get("type").asString()).isEqualTo("PONG");
        assertThat(c.poll(500)).isNull();
    }

    @Test
    void returnsErrorForMalformedJson() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));
        c.next(); // CONNECTED

        c.send("{not json");

        JsonNode error = c.next();
        assertThat(error.get("type").asString()).isEqualTo("ERROR");
        assertThat(error.get("payload").get("code").asString()).isEqualTo("INVALID_MESSAGE");
    }

    @Test
    void returnsErrorForMissingPayload() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));
        c.next(); // CONNECTED

        c.send("{\"type\":\"PING\",\"seq\":1}");

        JsonNode error = c.next();
        assertThat(error.get("payload").get("code").asString()).isEqualTo("INVALID_MESSAGE");
        assertThat(error.get("payload").get("refSeq").asLong()).isEqualTo(1);
    }

    @Test
    void returnsErrorForUnknownType() throws Exception {
        TestClient c = connect(jwtService.createAccessToken(accountId));
        c.next(); // CONNECTED

        c.send("{\"type\":\"FLY_TO_MOON\",\"seq\":7,\"payload\":{}}");

        JsonNode error = c.next();
        assertThat(error.get("type").asString()).isEqualTo("ERROR");
        assertThat(error.get("payload").get("code").asString()).isEqualTo("INVALID_MESSAGE");
        assertThat(error.get("payload").get("refSeq").asLong()).isEqualTo(7);
    }

    @Test
    void secondLoginKicksFirstConnectionAndKeepsSession() throws Exception {
        String token = jwtService.createAccessToken(accountId);
        TestClient first = connect(token);
        String firstSessionId = first.next().get("payload").get("sessionId").asString();

        TestClient second = connect(token);
        String secondSessionId = second.next().get("payload").get("sessionId").asString();

        JsonNode kick = first.next();
        assertThat(kick.get("type").asString()).isEqualTo("KICK");
        assertThat(kick.get("payload").get("reason").asString()).isEqualTo("DUPLICATE_LOGIN");
        assertThat(secondSessionId).isEqualTo(firstSessionId);
        assertThat(first.awaitClose(2000)).isTrue();
    }

    private TestClient connect(String token) throws Exception {
        String url = "ws://localhost:" + port + "/ws/game" + (token != null ? "?token=" + token : "");
        TestClient handler = new TestClient();
        WebSocketSession connection = client
                .execute(handler, new WebSocketHttpHeaders(), URI.create(url))
                .get(5, TimeUnit.SECONDS);
        openConnections.add(connection);
        handler.connection = connection;
        return handler;
    }

    private class TestClient extends TextWebSocketHandler {

        private final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        private final CountDownLatch closed = new CountDownLatch(1);
        private WebSocketSession connection;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            inbox.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            closed.countDown();
        }

        void send(String json) throws Exception {
            connection.sendMessage(new TextMessage(json));
        }

        JsonNode next() throws Exception {
            JsonNode message = poll(3000);
            assertThat(message).as("expected a server message").isNotNull();
            return message;
        }

        JsonNode poll(long timeoutMs) throws Exception {
            String raw = inbox.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return raw == null ? null : objectMapper.readTree(raw);
        }

        boolean awaitClose(long timeoutMs) throws InterruptedException {
            return closed.await(timeoutMs, TimeUnit.MILLISECONDS);
        }
    }
}
