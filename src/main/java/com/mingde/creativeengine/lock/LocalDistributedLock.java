package com.mingde.creativeengine.lock;

import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.exception.BusinessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 未开启 Redis 时的降级实现：按 key 哈希落到固定数量的分段锁上。
 *
 * <p>只在单实例内互斥，多实例部署必须开启 Redis 走 {@link RedissonDistributedLock}。</p>
 *
 * <p>用固定分段而不是 {@code Map<key, Lock>}：后者会随 key 数量无限增长，
 * 而分段锁只需 64 个对象就能把冲突概率压到很低。</p>
 */
public class LocalDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(LocalDistributedLock.class);

    /** 分段数量，取 2 的幂，冲突概率与 key 基数无关。 */
    private static final int STRIPE_COUNT = 64;

    private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

    public LocalDistributedLock() {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    @Override
    public <T> T execute(String key, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        ReentrantLock lock = stripes[Math.floorMod(key.hashCode(), STRIPE_COUNT)];
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("获取本地分段锁超时：{}", key);
                throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作过于频繁，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作被中断，请重试");
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
