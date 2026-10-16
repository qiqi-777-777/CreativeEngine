package com.mingde.creativeengine.security;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 开启 Redis 时的登录失败计数：多实例共用一份计数。
 *
 * <p>用 {@code INCR + EXPIRE} 实现，每次失败都续期，语义与本地实现对齐：
 * 窗口从最后一次失败起算。</p>
 */
public class RedisLoginAttemptGuard implements LoginAttemptGuard {

    /** 统一 key 前缀，便于在 Redis 里按命名空间清理。 */
    static final String KEY_PREFIX = "auth:login-fail:";

    private final StringRedisTemplate redisTemplate;
    private final Duration window;
    private final int maxAttempts;

    public RedisLoginAttemptGuard(StringRedisTemplate redisTemplate, Duration window, int maxAttempts) {
        this.redisTemplate = redisTemplate;
        this.window = window;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean isBlocked(String username) {
        String value = redisTemplate.opsForValue().get(key(username));
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= maxAttempts;
        } catch (NumberFormatException e) {
            // 脏数据按「没失败过」处理，避免把用户永久锁在门外
            redisTemplate.delete(key(username));
            return false;
        }
    }

    @Override
    public void recordFailure(String username) {
        String key = key(username);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, window);
    }

    @Override
    public void reset(String username) {
        redisTemplate.delete(key(username));
    }

    @Override
    public Duration window() {
        return window;
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
