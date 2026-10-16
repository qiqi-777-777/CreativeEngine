package com.mingde.creativeengine.lock;

import com.mingde.creativeengine.exception.BusinessException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进程内降级锁测试：验证正常执行、异常后释放、以及被其他线程持有时按超时快速失败。
 */
class LocalDistributedLockTest {

    private static final Duration WAIT = Duration.ofSeconds(2);
    private static final Duration LEASE = Duration.ofSeconds(5);

    private final LocalDistributedLock lock = new LocalDistributedLock();

    @Test
    void executesActionAndReturnsResult() {
        assertEquals("done", lock.execute("key-a", WAIT, LEASE, () -> "done"));
    }

    @Test
    void releasesLockEvenWhenActionThrows() {
        assertThrows(IllegalStateException.class, () -> lock.execute("key-b", WAIT, LEASE, () -> {
            throw new IllegalStateException("boom");
        }));

        // 抛异常后锁必须已释放，否则这次调用会等到超时并失败
        assertEquals("ok", lock.execute("key-b", WAIT, LEASE, () -> "ok"));
    }

    @Test
    void rejectsWhenKeyIsHeldByAnotherThread() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> lock.execute("key-c", WAIT, LEASE, () -> {
            held.countDown();
            awaitQuietly(release);
            return null;
        }));
        holder.start();
        assertTrue(held.await(2, TimeUnit.SECONDS), "占用线程应已拿到锁");

        try {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> lock.execute("key-c", Duration.ofMillis(200), LEASE, () -> "should-not-run"));
            assertTrue(exception.getMessage().contains("稍后重试"));
        } finally {
            release.countDown();
            holder.join(2000);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
