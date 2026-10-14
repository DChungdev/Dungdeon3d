package com.dungeondelvers.game.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Minimal game client for integration tests: sends envelopes with auto seq, queues replies. */
class WsTestClient extends TextWebSocketHandler implements AutoCloseable {

    private final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
    private final AtomicLong seq = new AtomicLong();
    private final ObjectMapper objectMapper;
    private WebSocketSession connection;

    private WsTestClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    static WsTestClient connect(int port, String token, ObjectMapper objectMapper) throws Exception {
        WsTestClient client = new WsTestClient(objectMapper);
        client.connection = new StandardWebSocketClient()
                .execute(client, new WebSocketHttpHeaders(),
                        URI.create("ws://localhost:" + port + "/ws/game?token=" + token))
                .get(5, TimeUnit.SECONDS);
        return client;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        inbox.add(message.getPayload());
    }

    /** Sends {@code {"type":type,"seq":next,"payload":payloadJson}}; returns the seq used. */
    long send(String type, String payloadJson) throws Exception {
        long s = seq.incrementAndGet();
        connection.sendMessage(new TextMessage(
                "{\"type\":\"" + type + "\",\"seq\":" + s + ",\"payload\":" + payloadJson + "}"));
        return s;
    }

    /** Next message of {@code type}, skipping others. Fails after 3 s. */
    JsonNode next(String type) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            String raw = remaining > 0 ? inbox.poll(remaining, TimeUnit.MILLISECONDS) : null;
            assertThat(raw).as("expected %s", type).isNotNull();
            JsonNode message = objectMapper.readTree(raw);
            if (type.equals(message.get("type").asString())) {
                return message.get("payload");
            }
        }
    }

    /** True if a message of {@code type} arrives within {@code timeoutMs}. */
    boolean receives(String type, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            String raw = remaining > 0 ? inbox.poll(remaining, TimeUnit.MILLISECONDS) : null;
            if (raw == null) {
                return false;
            }
            if (type.equals(objectMapper.readTree(raw).get("type").asString())) {
                return true;
            }
        }
    }

    boolean isOpen() {
        return connection != null && connection.isOpen();
    }

    void clearInbox() {
        inbox.clear();
    }

    @Override
    public void close() throws Exception {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}
