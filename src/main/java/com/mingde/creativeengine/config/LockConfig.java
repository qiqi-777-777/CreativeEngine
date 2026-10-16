package com.mingde.creativeengine.config;

import com.mingde.creativeengine.lock.DistributedLock;
import com.mingde.creativeengine.lock.LocalDistributedLock;
import com.mingde.creativeengine.lock.RedissonDistributedLock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 分布式锁装配。
 *
 * <p>Redisson 需要自己独立的客户端（Lettuce 的 RedisConnectionFactory 无法复用），
 * 连接参数直接复用 Spring Boot 的 {@code spring.data.redis.*}，避免 host/port/密码
 * 在两处各写一份导致不一致。只有 {@code app.redis.enabled=true} 时才创建，
 * 保证本地零依赖也能启动。Netty 版本由 Spring Boot 的依赖管理统一仲裁。</p>
 */
@Configuration
public class LockConfig {

    private static final Logger log = LoggerFactory.getLogger(LockConfig.class);

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }
        log.info("分布式锁启用 Redisson，连接 {}", server.getAddress());
        return Redisson.create(config);
    }

    @Bean
    public DistributedLock distributedLock(ObjectProvider<RedissonClient> redissonClients) {
        RedissonClient redissonClient = redissonClients.getIfAvailable();
        if (redissonClient != null) {
            return new RedissonDistributedLock(redissonClient);
        }
        log.info("未开启 Redis，分布式锁退化为进程内分段锁（仅单实例有效）");
        return new LocalDistributedLock();
    }
}
