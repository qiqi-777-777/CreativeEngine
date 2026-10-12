package com.mingde.creativeengine.config;

import com.mingde.creativeengine.storage.InMemoryTokenStore;
import com.mingde.creativeengine.storage.RedisTokenStore;
import com.mingde.creativeengine.storage.TokenStore;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            assertTrue(context.getBean(CacheManager.class) instanceof ConcurrentMapCacheManager,
                    "关闭 Redis 时应使用进程内缓存管理器");
        }
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
