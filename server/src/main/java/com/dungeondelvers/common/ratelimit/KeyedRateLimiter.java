package com.dungeondelvers.common.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link RateLimiter} per key (e.g. client IP + email), thread-safe. Expired windows are
 * evicted on access so the map can't grow without bound.
 */
public final class KeyedRateLimiter {

    private static final int EVICT_EVERY = 1024;

    private final int maxPerWindow;
    private final long windowMs;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private int callsSinceEvict;

    public KeyedRateLimiter(int maxPerWindow, long windowMs) {
        this.maxPerWindow = maxPerWindow;
        this.windowMs = windowMs;
    }

    public boolean tryAcquire(String key, long nowMs) {
        maybeEvict(nowMs);
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> new RateLimiter(maxPerWindow, windowMs));
        synchronized (limiter) {
            return limiter.tryAcquire(nowMs);
        }
    }

    private void maybeEvict(long nowMs) {
        synchronized (this) {
            if (++callsSinceEvict < EVICT_EVERY) {
                return;
            }
            callsSinceEvict = 0;
        }
        limiters.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isExpired(nowMs);
            }
        });
    }

    int size() {
        return limiters.size();
    }
}
