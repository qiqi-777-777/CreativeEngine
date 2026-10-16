package com.mingde.creativeengine.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 内存版 token 存储测试：重点是「不开启 Redis 时 token 也会过期」，
 * 以及 Bearer 前缀剥离、主动失效这些与 Redis 版一致的语义。
 */
class InMemoryTokenStoreTest {

    @Test
    void resolvesSavedTokenAndAcceptsBearerPrefix() {
        InMemoryTokenStore store = store(Duration.ofMinutes(10));

        store.save("abc123", 42L);

        assertEquals(42L, store.resolve("abc123"));
        assertEquals(42L, store.resolve("Bearer abc123"), "带 Bearer 前缀时应剥离后匹配");
    }

    @Test
    void expiresTokenAfterTtl() throws Exception {
        InMemoryTokenStore store = store(Duration.ofMillis(100));

        store.save("abc123", 42L);
        assertEquals(42L, store.resolve("abc123"), "TTL 内应可正常解析");

        Thread.sleep(300);

        assertNull(store.resolve("abc123"), "超过 TTL 后必须失效，否则登录凭证会永久有效");
    }

    @Test
    void revokeRemovesToken() {
        InMemoryTokenStore store = store(Duration.ofMinutes(10));
        store.save("abc123", 42L);

        store.revoke("Bearer abc123");

        assertNull(store.resolve("abc123"));
    }

    @Test
    void ignoresBlankTokenAndNullUser() {
        InMemoryTokenStore store = store(Duration.ofMinutes(10));

        store.save("", 42L);
        store.save("abc123", null);

        assertNull(store.resolve(""));
        assertNull(store.resolve("abc123"));
    }

    private static InMemoryTokenStore store(Duration ttl) {
        return new InMemoryTokenStore(ttl);
    }
}
