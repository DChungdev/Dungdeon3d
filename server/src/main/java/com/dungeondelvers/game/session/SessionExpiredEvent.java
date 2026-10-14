package com.dungeondelvers.game.session;

/**
 * Published when a disconnected session's reconnect grace period runs out.
 * Zone managers listen for this to remove the player from hubs / instances.
 */
public record SessionExpiredEvent(GameSession session) {}
