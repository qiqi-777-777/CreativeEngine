package com.mingde.creativeengine.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 版登录失败计数测试：验证自增 + 续期、阈值判断与脏数据兜底，无需真实 Redis。
 */
class RedisLoginAttemptGuardTest {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final String KEY = "auth:login-fail:alice";

    @SuppressWarnings("unchecked")
    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @Test
    void recordFailureIncrementsAndRefreshesExpiry() {
        when(template.opsForValue()).thenReturn(valueOperations);

        guard(3).recordFailure("alice");

        verify(valueOperations).increment(KEY);
        verify(template).expire(KEY, WINDOW);
    }

    @Test
    void blockedWhenCountReachesThreshold() {
        when(template.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(KEY)).thenReturn("3");
        assertTrue(guard(3).isBlocked("alice"));

        when(valueOperations.get(KEY)).thenReturn("2");
        assertFalse(guard(3).isBlocked("alice"));
    }

    @Test
    void unknownAccountIsNotBlocked() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertFalse(guard(3).isBlocked("alice"));
    }

    @Test
    void dirtyValueDoesNotLockUserForever() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("not-a-number");

        assertFalse(guard(3).isBlocked("alice"), "脏数据应视为未失败，而不是把用户永久锁住");
        verify(template).delete(KEY);
    }

    @Test
    void resetDeletesCounter() {
        guard(3).reset("alice");

        verify(template).delete(KEY);
    }

    private RedisLoginAttemptGuard guard(int maxAttempts) {
        return new RedisLoginAttemptGuard(template, WINDOW, maxAttempts);
    }
}
