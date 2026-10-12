package com.mingde.creativeengine.storage;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 版 token 存储测试：用 mock 的 StringRedisTemplate 验证读写与 key 规范，无需真实 Redis。
 */
class RedisTokenStoreTest {

    private static final Duration TTL = Duration.ofDays(7);

    @SuppressWarnings("unchecked")
    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final RedisTokenStore store = new RedisTokenStore(template, TTL);

    @Test
    void saveWritesUserIdWithTtl() {
        when(template.opsForValue()).thenReturn(valueOperations);

        store.save("token-abc", 42L);

        verify(valueOperations).set("auth:token:token-abc", "42", TTL);
    }

    @Test
    void resolveReturnsUserIdAndStripsBearerPrefix() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:token-abc")).thenReturn("42");

        assertEquals(42L, store.resolve("Bearer token-abc"));
        assertEquals(42L, store.resolve("token-abc"));
    }

    @Test
    void resolveReturnsNullWhenTokenMissing() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:unknown")).thenReturn(null);

        assertNull(store.resolve("unknown"));
    }

    @Test
    void resolveCleansUpIllegalValue() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:broken")).thenReturn("not-a-number");

        assertNull(store.resolve("broken"));
        verify(template).delete("auth:token:broken");
    }

    @Test
    void revokeDeletesKey() {
        store.revoke("token-abc");

        verify(template).delete("auth:token:token-abc");
    }
}
