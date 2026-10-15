package com.dungeondelvers.game.ws.handler;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Small payload parsing helpers shared by message handlers. */
final class HandlerSupport {

    private HandlerSupport() {}

    /** Returns the UUID in a JSON string node, or null if it is not a valid UUID string. */
    static UUID parseUuid(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        try {
            return UUID.fromString(node.asString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
