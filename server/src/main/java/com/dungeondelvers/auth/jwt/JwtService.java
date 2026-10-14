package com.dungeondelvers.auth.jwt;

import com.dungeondelvers.app.properties.DdoProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenTtlSec;

    public JwtService(DdoProperties properties) {
        byte[] keyBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSec = properties.jwt().accessTokenTtlSec();
    }

    public String createAccessToken(UUID accountId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenTtlSec);
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public UUID parseAccountId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }
        return UUID.fromString(claims.getSubject());
    }

    public long getAccessTokenTtlSec() {
        return accessTokenTtlSec;
    }
}
