package com.mingde.creativeengine.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 两级缓存测试：验证 L1 命中不再访问 L2、L1 未命中会回填、两层都未命中时只加载一次、写操作同时落到两层。
 */
class TwoLevelCacheTest {

    private static final String KEY = "policyList";

    private final Cache local = mock(Cache.class);
    private final Cache remote = mock(Cache.class);
    private final TwoLevelCache cache = new TwoLevelCache("policyList", local, remote);

    @Test
    void returnsLocalHitWithoutTouchingRemote() {
        when(local.get(KEY)).thenReturn(new SimpleValueWrapper("l1-value"));

        assertEquals("l1-value", cache.get(KEY).get());
        verify(remote, never()).get(any());
    }

    @Test
    void backfillsLocalWhenRemoteHits() {
        when(local.get(KEY)).thenReturn(null);
        when(remote.get(KEY)).thenReturn(new SimpleValueWrapper("l2-value"));

        assertEquals("l2-value", cache.get(KEY).get());
        verify(local).put(KEY, "l2-value");
    }

    @Test
    void loadsOnceAndWritesRemoteWhenBothMiss() throws Exception {
        AtomicInteger loadCount = new AtomicInteger();
        when(local.get(KEY)).thenReturn(null);
        when(remote.get(KEY)).thenReturn(null);
        // 模拟 Caffeine 的 loader 语义：把 key 交给装载函数
        when(local.get(eq(KEY), any(Callable.class))).thenAnswer(invocation ->
                ((Callable<?>) invocation.getArgument(1)).call());

        String value = cache.get(KEY, () -> {
            loadCount.incrementAndGet();
            return "loaded";
        });

        assertEquals("loaded", value);
        assertEquals(1, loadCount.get());
        verify(remote).put(KEY, "loaded");
    }

    @Test
    void reusesRemoteValueWithoutCallingLoaderAgain() throws Exception {
        AtomicInteger loadCount = new AtomicInteger();
        when(local.get(KEY)).thenReturn(null);
        when(remote.get(KEY)).thenReturn(new SimpleValueWrapper("l2-value"));

        String value = cache.get(KEY, () -> {
            loadCount.incrementAndGet();
            return "should-not-be-used";
        });

        assertEquals("l2-value", value);
        assertEquals(0, loadCount.get(), "L2 已有数据时不应该再查数据库");
    }

    @Test
    void putAndEvictTouchBothLevels() {
        cache.put(KEY, "v");
        verify(local).put(KEY, "v");
        verify(remote).put(KEY, "v");

        cache.evict(KEY);
        verify(local).evict(KEY);
        verify(remote).evict(KEY);
    }

    @Test
    void returnsNullWhenBothLevelsMiss() {
        when(local.get(KEY)).thenReturn(null);
        when(remote.get(KEY)).thenReturn(null);

        assertNull(cache.get(KEY));
    }
}
