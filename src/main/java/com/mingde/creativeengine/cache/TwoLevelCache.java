package com.mingde.creativeengine.cache;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * 两级缓存：L1 Caffeine（进程内、短 TTL）+ L2 Redis（跨实例共享、长 TTL）。
 *
 * <p>解决两个问题：</p>
 * <ol>
 *   <li><b>重复的网络往返</b>：政策列表、职业标签这类热点数据，改造前每次命中都要走一趟 Redis；
 *       放到 L1 后同实例内直接返回，L2 只在 L1 未命中或需要跨实例共享时兜底。</li>
 *   <li><b>缓存击穿</b>：L1 未命中时交给 Caffeine 的 {@code get(key, loader)} 语义，
 *       同一个 key 只会有一个线程真正执行加载逻辑，避免热点 key 失效瞬间把请求全打到数据库。</li>
 * </ol>
 *
 * <p>写操作（put / evict / clear）两层一起执行，避免出现「L1 已更新、L2 还是旧值」的不一致。</p>
 */
public class TwoLevelCache implements Cache {

    private final String name;
    private final Cache local;
    private final Cache remote;

    public TwoLevelCache(String name, Cache local, Cache remote) {
        this.name = name;
        this.local = local;
        this.remote = remote;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper localHit = local.get(key);
        if (localHit != null) {
            return localHit;
        }
        ValueWrapper remoteHit = remote.get(key);
        if (remoteHit != null) {
            // 回填 L1：同一实例后面的读取就不用再走 Redis 了
            local.put(key, remoteHit.get());
        }
        return remoteHit;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T localHit = local.get(key, type);
        if (localHit != null) {
            return localHit;
        }
        T remoteHit = remote.get(key, type);
        if (remoteHit != null) {
            local.put(key, remoteHit);
        }
        return remoteHit;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cached = get(key);
        if (cached != null) {
            return (T) cached.get();
        }
        // 两层都没有：交给 L1 的 loader 语义做单飞（single-flight）加载
        return local.get(key, () -> loadAndWriteRemote(key, valueLoader));
    }

    @Override
    public void put(Object key, Object value) {
        local.put(key, value);
        remote.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        local.evict(key);
        remote.evict(key);
    }

    @Override
    public void clear() {
        local.clear();
        remote.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T loadAndWriteRemote(Object key, Callable<T> valueLoader) throws Exception {
        // L1 未命中不代表别的实例也没有，先再确认一次 L2
        ValueWrapper remoteHit = remote.get(key);
        if (remoteHit != null) {
            return (T) remoteHit.get();
        }
        T loaded = valueLoader.call();
        if (loaded != null) {
            remote.put(key, loaded);
        }
        return loaded;
    }
}
