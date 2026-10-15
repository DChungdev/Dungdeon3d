package com.dungeondelvers.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final long T = 1_791_000_000_000L; // realistic epoch millis

    @Test
    void allowsLimitPerWindowAndResetsEverySecond() {
        RateLimiter limiter = new RateLimiter(3);

        assertThat(limiter.tryAcquire(T)).isTrue();
        assertThat(limiter.tryAcquire(T + 10)).isTrue();
        assertThat(limiter.tryAcquire(T + 20)).isTrue();
        assertThat(limiter.tryAcquire(T + 30)).isFalse();

        assertThat(limiter.tryAcquire(T + 1000)).isTrue();
        assertThat(limiter.tryAcquire(T + 5000)).isTrue();
    }

    @Test
    void customWindow() {
        RateLimiter perMinute = new RateLimiter(5, 60_000);
        for (int i = 0; i < 5; i++) {
            assertThat(perMinute.tryAcquire(T + i * 1000L)).isTrue();
        }
        assertThat(perMinute.tryAcquire(T + 59_000)).isFalse();
        assertThat(perMinute.tryAcquire(T + 60_000)).isTrue();
    }

    @Test
    void keyedLimiterIsolatesKeys() {
        KeyedRateLimiter limiter = new KeyedRateLimiter(2, 60_000);

        assertThat(limiter.tryAcquire("1.2.3.4|a@x.dev", T)).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4|a@x.dev", T)).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4|a@x.dev", T)).isFalse();
        assertThat(limiter.tryAcquire("1.2.3.4|b@x.dev", T)).isTrue();
    }

    @Test
    void keyedLimiterEvictsExpiredWindows() {
        KeyedRateLimiter limiter = new KeyedRateLimiter(1, 1000);
        for (int i = 0; i < 1023; i++) {
            limiter.tryAcquire("key" + i, T);
        }
        assertThat(limiter.size()).isEqualTo(1023);

        limiter.tryAcquire("late", T + 5000); // 1024th call triggers eviction

        assertThat(limiter.size()).isEqualTo(1);
    }
}
