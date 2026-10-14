package com.dungeondelvers.game.ws.protocol;

import tools.jackson.databind.JsonNode;

/**
 * Client → Server envelope. See docs/network-protocol.md §2.1.
 */
public record ClientMessage(
        String type,
        long seq,
        Long ts,
        JsonNode payload
) {

    public boolean isWellFormed() {
        return type != null && !type.isBlank()
                && seq > 0
                && payload != null && payload.isObject();
    }
}
