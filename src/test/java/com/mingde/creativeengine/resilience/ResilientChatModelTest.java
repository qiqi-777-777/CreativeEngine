package com.mingde.creativeengine.resilience;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 大模型保护层测试：验证正常放行、熔断开后不再打模型、限流拒绝不计入熔断统计。
 *
 * <p>全部用程序化配置构造熔断器与限流器，不依赖 Spring 容器与网络。</p>
 */
class ResilientChatModelTest {

    private static final ChatRequest REQUEST = ChatRequest.builder()
            .messages(UserMessage.from("你好"))
            .build();

    @Test
    void passesResponseThroughWhenModelIsHealthy() {
        ResilientChatModel model = new ResilientChatModel(
                stub(request -> response("ok")), defaultCircuitBreaker(), permissiveRateLimiter());

        assertEquals("ok", model.chat(REQUEST).aiMessage().text());
    }

    @Test
    void opensCircuitAfterFailureThresholdAndStopsCallingModel() {
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(5))
                .build()).circuitBreaker("test");

        AtomicInteger modelCalls = new AtomicInteger();
        ResilientChatModel model = new ResilientChatModel(
                stub(request -> {
                    modelCalls.incrementAndGet();
                    throw new IllegalStateException("DeepSeek 502");
                }),
                circuitBreaker,
                permissiveRateLimiter());

        assertThrows(IllegalStateException.class, () -> model.chat(REQUEST));
        assertThrows(IllegalStateException.class, () -> model.chat(REQUEST));
        assertEquals(2, modelCalls.get());

        assertThrows(CallNotPermittedException.class, () -> model.chat(REQUEST));
        assertEquals(2, modelCalls.get(), "熔断打开后请求应被直接拒绝，不再打到模型");
    }

    @Test
    void rejectsWhenRateLimitExhausted() {
        ResilientChatModel model = new ResilientChatModel(
                stub(request -> response("ok")), defaultCircuitBreaker(), strictRateLimiter());

        assertEquals("ok", model.chat(REQUEST).aiMessage().text());
        assertThrows(RequestNotPermitted.class, () -> model.chat(REQUEST));
    }

    @Test
    void rateLimitRejectionDoesNotOpenCircuitBreaker() {
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .build()).circuitBreaker("test");

        ResilientChatModel model = new ResilientChatModel(
                stub(request -> response("ok")), circuitBreaker, strictRateLimiter());

        model.chat(REQUEST);
        assertThrows(RequestNotPermitted.class, () -> model.chat(REQUEST));
        assertThrows(RequestNotPermitted.class, () -> model.chat(REQUEST));

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(),
                "限流属于主动丢弃，不应被熔断器统计成模型故障");
    }

    private static ChatModel stub(Function<ChatRequest, ChatResponse> handler) {
        return new ChatModel() {
            @Override
            public ChatResponse chat(ChatRequest chatRequest) {
                return handler.apply(chatRequest);
            }
        };
    }

    private static ChatResponse response(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }

    private static CircuitBreaker defaultCircuitBreaker() {
        return CircuitBreakerRegistry.ofDefaults().circuitBreaker("test");
    }

    /** 每秒 50 次许可，测试里不会触发限流。 */
    private static RateLimiter permissiveRateLimiter() {
        return RateLimiterRegistry.ofDefaults().rateLimiter("test");
    }

    /** 一分钟只放 1 次，且拿不到许可立即失败，用来稳定复现限流。 */
    private static RateLimiter strictRateLimiter() {
        return RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build()).rateLimiter("test");
    }
}
