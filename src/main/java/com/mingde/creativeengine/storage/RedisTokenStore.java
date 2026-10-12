package com.mingde.creativeengine.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 版 token 存储。
 *
 * <p>相比内存实现带来的收益：</p>
 * <ul>
 *   <li><b>多实例共享</b>：多台后端共用一份登录态，网关/负载均衡下不会出现"登录成功却提示未登录"；</li>
 *   <li><b>重启不丢</b>：进程重启或发版后用户无需重新登录；</li>
 *   <li><b>自动过期</b>：TTL 到期自动回收，避免静态 Map 无限增长导致内存泄漏。</li>
 * </ul>
 */
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    /** 统一 key 前缀，便于在 Redis 中按命名空间清理。 */
    static final String KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisTokenStore(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public void save(String token, Long userId) {
        if (token == null || token.isEmpty() || userId == null) {
            return;
        }
        redisTemplate.opsForValue().set(key(token), String.valueOf(userId), ttl);
        log.debug("token 已写入 Redis，ttl={}s", ttl.toSeconds());
    }

    @Override
    public Long resolve(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(key(token));
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            // 脏数据直接忽略并清理，避免持续报错
            log.warn("Redis 中的 token 记录非法，已清理：{}", value);
            revoke(token);
            return null;
        }
    }

    @Override
    public void revoke(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return KEY_PREFIX + InMemoryTokenStore.stripBearer(token);
    }
}
