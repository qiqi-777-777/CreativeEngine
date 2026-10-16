package com.mingde.creativeengine.resilience;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.util.function.Supplier;

/**
 * 大模型调用的保护层：限流 + 熔断。
 *
 * <p>为什么装饰在 {@link ChatModel} 这一层，而不是在业务代码里逐个 try/catch：
 * 模型调用散落在 5 个 Agent（政策/职业/BP/创业/路由）和两条链路（通用与 JSON 输出）上，
 * 在这里包一层，所有场景自动生效，业务代码零改动。</p>
 *
 * <p><b>顺序上必须先限流、再熔断</b>：限流拒绝属于"主动丢弃"，如果被熔断器统计成模型故障，
 * 一波突发流量就会把熔断器误判成打开。所以 {@code RateLimiter} 在外层，
 * {@code RequestNotPermitted} 不会进入熔断器的统计窗口。</p>
 *
 * <p><b>重试不在这里做</b>：OpenAI 客户端本身已按 {@code deepseek.max-retries} 重试过，
 * 外面再叠一层会把一次用户请求放大成多次模型调用，反而更容易撞上限流。</p>
 */
public class ResilientChatModel implements ChatModel {

    /** 熔断器与限流器的实例名，对应 application.yml 中 resilience4j.*.instances.deepseek。 */
    public static final String INSTANCE_NAME = "deepseek";

    private final ChatModel delegate;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public ResilientChatModel(ChatModel delegate, CircuitBreaker circuitBreaker, RateLimiter rateLimiter) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 覆盖 {@code chat(ChatRequest)}：接口里的 {@code chat(String)} / {@code chat(List)} 等重载
     * 最终都会走到这里，因此这一处即覆盖全部同步调用路径。
     *
     * <p>注意要委托给 {@code delegate.chat(...)} 而不是 {@code doChat(...)}，
     * 否则会绕过代理内部的监听器编排，AgentTelemetry 的模型调用指标与日志就丢了。</p>
     */
    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        Supplier<ChatResponse> callWithCircuitBreaker =
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> delegate.chat(chatRequest));
        return RateLimiter.decorateSupplier(rateLimiter, callWithCircuitBreaker).get();
    }
}
