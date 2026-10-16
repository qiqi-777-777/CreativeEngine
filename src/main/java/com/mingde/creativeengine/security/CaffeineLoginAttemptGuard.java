package com.mingde.creativeengine.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * 未开启 Redis 时的登录失败计数：放在进程内，单机有效。
 *
 * <p>用 {@code expireAfterWrite}，而且每次失败都会写一次，因此窗口从「最后一次失败」
 * 重新起算——持续的暴力尝试会一直被锁在门外，正常用户停手后窗口自然过期。</p>
 */
public class CaffeineLoginAttemptGuard implements LoginAttemptGuard {

    private final Duration window;
    private final int maxAttempts;
    private final Cache<String, Integer> failures;

    public CaffeineLoginAttemptGuard(Duration window, int maxAttempts) {
        this.window = window;
        this.maxAttempts = maxAttempts;
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(MAX_TRACKED_ACCOUNTS)
                .build();
    }

    @Override
    public boolean isBlocked(String username) {
        Integer count = failures.getIfPresent(username);
        return count != null && count >= maxAttempts;
    }

    @Override
    public void recordFailure(String username) {
        failures.asMap().merge(username, 1, Integer::sum);
    }

    @Override
    public void reset(String username) {
        failures.invalidate(username);
    }

    @Override
    public Duration window() {
        return window;
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }
}
