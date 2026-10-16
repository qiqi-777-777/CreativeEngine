package com.mingde.creativeengine.lock;

import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.exception.BusinessException;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的分布式锁：多实例部署时保证同一把锁全局只有一个持有者。
 *
 * <p>不自己用 {@code SET NX} + {@code DEL} 手写的原因：Redisson 的 RLock 底层是
 * 「Hash 结构 + Lua 脚本 + 看门狗」，额外提供了可重入、原子释放与自动续期，
 * 手写版本在"业务超时后误删别人的锁"这类场景上很容易出错。</p>
 */
public class RedissonDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedissonDistributedLock.class);

    private final RedissonClient redissonClient;

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String key, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("获取分布式锁超时：{}", key);
                throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作过于频繁，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            // 恢复中断标记，交给容器结束当前请求
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "操作被中断，请重试");
        } finally {
            // 只有确实持有锁的线程才能解锁，避免误放别人的锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
