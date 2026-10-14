package com.dungeondelvers.app.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ddo")
public record DdoProperties(
        JwtProperties jwt,
        CorsProperties cors,
        GameProperties game,
        CacheProperties cache
) {
    public record JwtProperties(
            String secret,
            long accessTokenTtlSec,
            long refreshTokenTtlSec
    ) {}

    public record CorsProperties(String allowedOrigins) {}

    public record GameProperties(
            int hubMaxPlayersPerShard,
            int instanceMaxPlayers,
            int instanceIdleTimeoutSec,
            int combatTickHz,
            int reconnectGraceSec
    ) {}

    public record CacheProperties(String type) {}
}
