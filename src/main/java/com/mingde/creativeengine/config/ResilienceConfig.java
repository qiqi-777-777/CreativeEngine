package com.mingde.creativeengine.config;

import com.mingde.creativeengine.resilience.ResilientChatModel;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型调用保护配置。
 *
 * <p>实例参数（滑动窗口、失败率阈值、每秒许可数等）写在 application.yml 的
 * {@code resilience4j.*.instances.deepseek} 下，由 resilience4j-spring-boot3 自动装配的
 * Registry 读取，这里只负责取出实例并挂上状态变化日志。</p>
 *
 * <p>两个 ChatModel（通用 / JSON）共用同一个熔断器与限流器，即整站模型调用共享一份配额，
 * 否则两条链路各算一份，总量会被放大。</p>
 */
@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreaker modelCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(ResilientChatModel.INSTANCE_NAME);
        // 熔断器开合是"模型服务健康度"最关键的信号，单独打一条日志便于告警与排障
        circuitBreaker.getEventPublisher().onStateTransition(event -> log.warn(
                "大模型熔断器 {} -> {}（实例：{}）",
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState(),
                event.getCircuitBreakerName()));
        return circuitBreaker;
    }

    @Bean
    public RateLimiter modelRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter(ResilientChatModel.INSTANCE_NAME);
    }
}
