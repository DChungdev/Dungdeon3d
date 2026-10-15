package com.dungeondelvers.game.ws.protocol;

/**
 * WebSocket message type names. Kept as String constants (not an enum) so unknown
 * types from newer clients can be rejected with an ERROR instead of failing to parse.
 */
public final class MessageType {

    private MessageType() {}

    // Connection
    public static final String CONNECTED = "CONNECTED";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String ACK = "ACK";
    public static final String ERROR = "ERROR";
    public static final String KICK = "KICK";
    public static final String RECONNECT = "RECONNECT";

    // Instance
    public static final String JOIN_INSTANCE = "JOIN_INSTANCE";
    public static final String LEAVE_INSTANCE = "LEAVE_INSTANCE";
    public static final String INSTANCE_ENTER = "INSTANCE_ENTER";
    public static final String MOVE_INTENT = "MOVE_INTENT";
    public static final String ENTITY_UPDATE = "ENTITY_UPDATE";
    public static final String ENTITY_SPAWN = "ENTITY_SPAWN";
    public static final String ENTITY_DESPAWN = "ENTITY_DESPAWN";

    // Combat
    public static final String AUTO_ATTACK_TARGET = "AUTO_ATTACK_TARGET";
    public static final String SKILL_CAST = "SKILL_CAST";
    public static final String SKILL_RESULT = "SKILL_RESULT";
    public static final String DAMAGE_EVENT = "DAMAGE_EVENT";
    public static final String ENTITY_DEATH = "ENTITY_DEATH";

    // Loot
    public static final String LOOT_DROP = "LOOT_DROP";
}
