package com.dungeondelvers.game.ws.protocol;

import java.util.UUID;

/**
 * Server → Client payload bodies for connection-level messages.
 */
public final class Payloads {

    private Payloads() {}

    public record Connected(UUID sessionId, long serverTime, String configVersion, UUID characterId) {}

    public record Pong(long serverTime) {}

    public record Error(String code, String message, Long refSeq) {}

    public record Kick(String reason, String message) {}
}
