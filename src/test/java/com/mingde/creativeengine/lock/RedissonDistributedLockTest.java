package com.mingde.creativeengine.lock;

import com.mingde.creativeengine.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redisson 分布式锁测试：用 mock 的 RLock 验证加解锁时机与抢锁失败的处理，无需真实 Redis。
 */
class RedissonDistributedLockTest {

    private static final Duration WAIT = Duration.ofSeconds(3);
    private static final Duration LEASE = Duration.ofSeconds(10);

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final RedissonDistributedLock distributedLock = new RedissonDistributedLock(redissonClient);

    @Test
    void executesActionThenUnlocks() throws Exception {
        when(redissonClient.getLock("k")).thenReturn(lock);
        when(lock.tryLock(WAIT.toMillis(), LEASE.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertEquals("ok", distributedLock.execute("k", WAIT, LEASE, () -> "ok"));

        verify(lock).unlock();
    }

    @Test
    void unlocksWhenActionThrows() throws Exception {
        when(redissonClient.getLock("k")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> distributedLock.execute("k", WAIT, LEASE, () -> {
            throw new IllegalStateException("boom");
        }));

        verify(lock).unlock();
    }

    @Test
    void failsFastWhenLockNotAcquired() throws Exception {
        when(redissonClient.getLock("k")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        AtomicBoolean actionCalled = new AtomicBoolean(false);

        assertThrows(BusinessException.class, () -> distributedLock.execute("k", WAIT, LEASE, () -> {
            actionCalled.set(true);
            return "never";
        }));

        assertFalse(actionCalled.get(), "没抢到锁时不应执行业务逻辑");
        verify(lock, never()).unlock();
    }
}
