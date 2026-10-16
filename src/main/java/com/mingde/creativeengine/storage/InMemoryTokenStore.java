package com.mingde.creativeengine.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * 单机内存版 token 存储，作为未开启 Redis 时的默认实现。
 *
 * <p>改造前是裸 {@code ConcurrentHashMap}，读写都不带过期，于是
 * {@code app.redis.enabled=false}（默认）时登录凭证永久有效——只能靠重启服务失效。
 * 现在换成 Caffeine：TTL 与 Redis 版保持一致，同时带上容量上限，避免长期运行下无限增长。</p>
 *
 * <p>用 {@code expireAfterWrite} 而不是 {@code expireAfterAccess}，是为了和
 * {@code RedisTokenStore} 的 {@code SET ... EX} 语义对齐：都是「从签发时刻起算到底」，
 * 不会因为用户一直活跃就无限续期。</p>
 */
public class InMemoryTokenStore implements TokenStore {

    /** 单机内存里的 token 条数上限，纯兜底，防止异常流量把内存吃满。 */
    private static final int MAX_ENTRIES = 10_000;

    private final Cache<String, Long> tokenStore;

    public InMemoryTokenStore(Duration ttl) {
        this.tokenStore = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(MAX_ENTRIES)
                .build();
    }

    @Override
    public void save(String token, Long userId) {
        if (token == null || token.isEmpty() || userId == null) {
            return;
        }
        tokenStore.put(stripBearer(token), userId);
    }

    @Override
    public Long resolve(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return tokenStore.getIfPresent(stripBearer(token));
    }

    @Override
    public void revoke(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        tokenStore.invalidate(stripBearer(token));
    }

    static String stripBearer(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
