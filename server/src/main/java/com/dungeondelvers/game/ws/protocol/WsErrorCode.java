package com.dungeondelvers.game.ws.protocol;

/**
 * Error codes sent in ERROR payloads. See docs/network-protocol.md §3.
 */
public enum WsErrorCode {
    INVALID_MESSAGE,
    UNAUTHORIZED,
    NOT_IN_ZONE,
    SKILL_ON_COOLDOWN,
    INSUFFICIENT_MP,
    TARGET_INVALID,
    RATE_LIMITED,
    PARTY_NOT_LEADER,
    INSTANCE_NOT_FOUND,
    MAINTENANCE,
    INTERNAL_ERROR
}
