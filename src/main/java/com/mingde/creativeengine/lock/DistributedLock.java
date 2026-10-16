package com.mingde.creativeengine.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁。
 *
 * <p>抽象出来的理由和 {@code TokenStore} 一致：本地开发默认不开 Redis，
 * 这时退化为进程内的分段锁，业务代码不需要感知当前跑在哪种部署形态上。</p>
 */
public interface DistributedLock {

    /**
     * 在锁保护下执行 action。
     *
     * @param key       锁的键，必须包含业务维度，避免不同业务之间互相阻塞
     * @param waitTime  抢锁最长等待时间，超时抛出业务异常而不是无限阻塞
     * @param leaseTime 持锁上限，业务线程异常退出时也能自动释放，避免死锁
     * @param action    临界区逻辑
     * @return action 的返回值
     */
    <T> T execute(String key, Duration waitTime, Duration leaseTime, Supplier<T> action);
}
