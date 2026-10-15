package com.dungeondelvers.common.ratelimit;

/**
 * Fixed-window counter (network-protocol §9). Not thread-safe: callers serialize access
 * (instance rooms lock their entities; {@link KeyedRateLimiter} synchronizes per key).
 */
public final class RateLimiter {

    private final int maxPerWindow;
    private final long windowMs;
    private boolean started;
    private long windowStartMs;
    private int count;

    /** {@code maxPerSecond} events per one-second window. */
    public RateLimiter(int maxPerSecond) {
        this(maxPerSecond, 1000);
    }

    public RateLimiter(int maxPerWindow, long windowMs) {
        this.maxPerWindow = maxPerWindow;
        this.windowMs = windowMs;
    }

    /** Counts one event; returns false if it exceeds the limit for the current window. */
    public boolean tryAcquire(long nowMs) {
        // An explicit flag rather than a sentinel start time: "now - Long.MIN_VALUE" overflows.
        if (!started || nowMs - windowStartMs >= windowMs) {
            started = true;
            windowStartMs = nowMs;
            count = 0;
        }
        return ++count <= maxPerWindow;
    }

    /** Events counted in the current window, including rejected ones. */
    public int countInWindow() {
        return count;
    }

    /** True once the window this limiter last saw has ended (safe to evict). */
    public boolean isExpired(long nowMs) {
        return !started || nowMs - windowStartMs >= windowMs;
    }
}
