package com.mingde.creativeengine.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mingde.creativeengine.storage.InMemoryTokenStore;
import com.mingde.creativeengine.storage.RedisTokenStore;
import com.mingde.creativeengine.storage.TokenStore;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
     * <p>开启 Redis 时用 {@link RedisCacheManager}（带 TTL 与 JSON 序列化）；
     * 否则用进程内 {@link ConcurrentMapCacheManager}，接口签名与 @Cacheable 注解保持不变。</p>
     */
    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactories,
                                     @Value("${app.redis.enabled:false}") boolean redisEnabled,
                                     @Value("${app.redis.hot-data-ttl:10m}") String hotDataTtl) {
        RedisConnectionFactory connectionFactory = connectionFactories.getIfAvailable();
        if (redisEnabled && connectionFactory != null) {
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(redisCacheConfiguration(parseTtl(hotDataTtl, Duration.ofMinutes(10))))
                    .build();
        }
        return new ConcurrentMapCacheManager();
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
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
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
