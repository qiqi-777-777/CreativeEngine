package com.mingde.creativeengine.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mingde.creativeengine.cache.TwoLevelCacheManager;
import com.mingde.creativeengine.storage.InMemoryTokenStore;
import com.mingde.creativeengine.storage.RedisTokenStore;
import com.mingde.creativeengine.storage.TokenStore;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 相关装配：登录 token 存储与热点数据缓存。
 *
 * <p>统一由 {@code app.redis.enabled} 控制：为 false（默认）时全部退化为单机内存实现，
 * 保证本地开发、单元测试零依赖即可运行；为 true 且 Redis 客户端可用时切换为 Redis 实现，
 * 获得多实例共享、重启不丢与自动过期的收益。</p>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 登录 token 存储：开启 Redis 时多实例共享登录态并自动过期。
     */
    @Bean
    public TokenStore tokenStore(ObjectProvider<StringRedisTemplate> redisTemplates,
                                 @Value("${app.redis.enabled:false}") boolean redisEnabled,
                                 @Value("${app.redis.token-ttl:7d}") String tokenTtl) {
        StringRedisTemplate redisTemplate = redisTemplates.getIfAvailable();
        if (redisEnabled && redisTemplate != null) {
            return new RedisTokenStore(redisTemplate, parseTtl(tokenTtl, Duration.ofDays(7)));
        }
        return new InMemoryTokenStore();
    }

    /**
     * 缓存管理器：承载政策列表、政策详情、职业标签等读多写少的热点数据。
     *
     * <p>整体是两级结构：</p>
     * <ul>
     *   <li><b>L1 Caffeine</b>：进程内、短 TTL、带容量上限，挡住绝大多数重复读，省掉 Redis 网络往返；</li>
     *   <li><b>L2 Redis</b>：开启 {@code app.redis.enabled} 时用 {@link RedisCacheManager}，
     *       提供跨实例共享与较长 TTL；关闭时退化为进程内实现，保证本地零依赖也能跑。</li>
     * </ul>
     */
    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactories,
                                     @Value("${app.redis.enabled:false}") boolean redisEnabled,
                                     @Value("${app.redis.hot-data-ttl:10m}") String hotDataTtl,
                                     @Value("${app.cache.local-ttl:60s}") String localTtl,
                                     @Value("${app.cache.local-max-size:1000}") long localMaxSize) {
        CaffeineCacheManager localManager = new CaffeineCacheManager();
        localManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(parseTtl(localTtl, Duration.ofSeconds(60)))
                .maximumSize(localMaxSize));

        RedisConnectionFactory connectionFactory = connectionFactories.getIfAvailable();
        CacheManager remoteManager = redisEnabled && connectionFactory != null
                ? RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(redisCacheConfiguration(parseTtl(hotDataTtl, Duration.ofMinutes(10))))
                        .build()
                : new ConcurrentMapCacheManager();

        return new TwoLevelCacheManager(localManager, remoteManager);
    }

    private RedisCacheConfiguration redisCacheConfiguration(Duration ttl) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 缓存值是 List<PolicyData> 等集合，需要写入类型信息才能正确反序列化
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                // 写入时给 TTL 加 ±10% 抖动，避免同一批热点缓存同时失效、请求瞬间全打到数据库（缓存雪崩）
                .entryTtl((key, value) -> jitter(ttl))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    /**
     * 给 TTL 加上 ±10% 的随机抖动，用于打散过期时间、避免缓存雪崩。
     */
    static Duration jitter(Duration base) {
        long baseMillis = base.toMillis();
        if (baseMillis <= 1) {
            return base;
        }
        long delta = Math.max(1, Math.round(baseMillis * 0.1));
        long millis = baseMillis + ThreadLocalRandom.current().nextLong(-delta, delta + 1);
        return Duration.ofMillis(Math.max(1, millis));
    }

    /**
     * 解析 {@code 7d / 30m / 10s} 形式的 TTL 配置；格式非法时回退到默认值。
     *
     * <p>供本配置与 {@link LangChain4jConfig} 的会话记忆共用，避免两处各写一份解析逻辑。</p>
     */
    static Duration parseTtl(String raw, Duration fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return DurationStyle.detectAndParse(raw.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
