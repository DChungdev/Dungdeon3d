package com.dungeondelvers.auth.jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

    private record Entry(UUID accountId, Instant expiresAt) {}

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public String issue(UUID accountId, long ttlSec) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new Entry(accountId, Instant.now().plusSeconds(ttlSec)));
        return token;
    }

    public Optional<UUID> consume(String token) {
        Entry entry = tokens.remove(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.accountId());
    }

    public void revokeAllForAccount(UUID accountId) {
        tokens.entrySet().removeIf(e -> e.getValue().accountId().equals(accountId));
    }
}
