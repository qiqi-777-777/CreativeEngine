package com.mingde.creativeengine.config;

import com.mingde.creativeengine.cache.TwoLevelCacheManager;
import com.mingde.creativeengine.storage.InMemoryTokenStore;
import com.mingde.creativeengine.storage.RedisTokenStore;
import com.mingde.creativeengine.storage.TokenStore;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Redis 装配测试：验证开关为关时退化为内存实现、为开且有客户端时切到 Redis 实现。
 */
class RedisConfigTest {

    @Test
    void redisDisabledUsesInMemoryImplementations() {
        try (AnnotationConfigApplicationContext context = context("false")) {
            assertTrue(context.getBean(TokenStore.class) instanceof InMemoryTokenStore,
                    "关闭 Redis 时应使用内存版 token 存储");
            CacheManager cacheManager = context.getBean(CacheManager.class);
            assertTrue(cacheManager instanceof TwoLevelCacheManager,
                    "缓存管理器应是 L1 本地 + L2 远端的组合");
            assertNotNull(cacheManager.getCache("policyList"), "缓存名应按需创建");
        }
    }

    @Test
    void jitterKeepsTtlWithinTenPercent() {
        Duration base = Duration.ofMinutes(10);

        for (int i = 0; i < 100; i++) {
            Duration jittered = RedisConfig.jitter(base);
            assertTrue(jittered.compareTo(Duration.ofMinutes(9)) >= 0,
                    "抖动后不应低于基准的 90%，实际：" + jittered);
            assertTrue(jittered.compareTo(Duration.ofMinutes(11)) <= 0,
                    "抖动后不应高于基准的 110%，实际：" + jittered);
        }

        assertEquals(Duration.ZERO, RedisConfig.jitter(Duration.ZERO), "过短的 TTL 不做抖动");
    }

    @Test
    void redisEnabledWithClientUsesRedisTokenStore() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment("true"));
        context.register(RedisConfig.class);
        context.registerBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

        try {
            context.refresh();
            assertTrue(context.getBean(TokenStore.class) instanceof RedisTokenStore,
                    "开启 Redis 且有客户端时应使用 Redis 版 token 存储");
        } finally {
            context.close();
        }
    }

    @Test
    void parseTtlSupportsCommonUnitsAndFallsBack() {
        Duration fallback = Duration.ofSeconds(1);

        assertEquals(Duration.ofMinutes(30), RedisConfig.parseTtl("30m", fallback));
        assertEquals(Duration.ofDays(7), RedisConfig.parseTtl("7d", fallback));
        assertEquals(Duration.ofHours(2), RedisConfig.parseTtl("2h", fallback));
        assertEquals(fallback, RedisConfig.parseTtl("bogus", fallback));
        assertEquals(fallback, RedisConfig.parseTtl(null, fallback));
        assertEquals(fallback, RedisConfig.parseTtl("  ", fallback));
    }

    private AnnotationConfigApplicationContext context(String redisEnabled) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment(redisEnabled));
        context.register(RedisConfig.class);
        context.refresh();
        return context;
    }

    private StandardEnvironment environment(String redisEnabled) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("app.redis.enabled", redisEnabled)));
        return environment;
    }
}
