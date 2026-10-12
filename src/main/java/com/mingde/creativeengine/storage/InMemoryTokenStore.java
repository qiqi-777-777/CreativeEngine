package com.mingde.creativeengine.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单机内存版 token 存储，作为未开启 Redis 时的默认实现。
 *
 * <p>保留原有的进程内行为，保证本地零依赖也能正常启动和联调；缺点同样是重启失效、
 * 多实例不共享，因此生产环境应开启 {@code app.redis.enabled}。</p>
 */
public class InMemoryTokenStore implements TokenStore {

    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

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
        return tokenStore.get(stripBearer(token));
    }

    @Override
    public void revoke(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        tokenStore.remove(stripBearer(token));
    }

    static String stripBearer(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
