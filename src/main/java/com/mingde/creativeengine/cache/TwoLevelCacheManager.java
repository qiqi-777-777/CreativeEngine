package com.mingde.creativeengine.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 把两个 {@link CacheManager} 组合成「本地 + 远端」两级缓存：
 * L1 取自 {@code localManager}（Caffeine），L2 取自 {@code remoteManager}（Redis 或进程内实现）。
 *
 * <p>对上层完全透明：{@code @Cacheable} 注解与缓存名都不用改，只换掉注入的 CacheManager 即可。</p>
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager localManager;
    private final CacheManager remoteManager;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager localManager, CacheManager remoteManager) {
        this.localManager = localManager;
        this.remoteManager = remoteManager;
    }

    @Override
    public Cache getCache(String name) {
        Cache cached = caches.get(name);
        if (cached != null) {
            return cached;
        }
        Cache local = localManager.getCache(name);
        Cache remote = remoteManager.getCache(name);
        if (local == null || remote == null) {
            return null;
        }
        TwoLevelCache twoLevel = new TwoLevelCache(name, local, remote);
        // 并发首次访问同一个缓存名时保证只创建一个包装对象
        Cache existing = caches.putIfAbsent(name, twoLevel);
        return existing == null ? twoLevel : existing;
    }

    @Override
    public Set<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>(localManager.getCacheNames());
        names.addAll(remoteManager.getCacheNames());
        return names;
    }
}
