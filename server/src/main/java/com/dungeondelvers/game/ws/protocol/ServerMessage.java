package com.dungeondelvers.game.ws.protocol;

/**
 * Server → Client envelope. See docs/network-protocol.md §2.2.
 */
public record ServerMessage(
        String type,
        long seq,
        long serverTs,
        boolean ackRequired,
        Object payload
) {}
