package com.mingde.creativeengine.observability;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.ToolExecution;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 大模型与工具调用的可观测性。
 *
 * <p>改造前只有业务日志，出了问题无法回答"这次问答模型调了几次、花了多少 token、哪个工具慢"。
 * 这里把 LangChain4j 的 {@code ChatModelListener}（模型调用）和 {@code afterToolExecution}
 * （工具调用）统一接到 Micrometer：</p>
 *
 * <ul>
 *   <li>{@code agent_model_call_seconds}：模型调用耗时，按 成功/失败 打标签；</li>
 *   <li>{@code agent_model_tokens_total}：输入/输出 token 计数，用于估算成本；</li>
 *   <li>{@code agent_tool_call_seconds}：工具调用耗时，按工具名与成功/失败打标签。</li>
 * </ul>
 *
 * <p>日志里统一带上 traceId，方便把一次问答里的多次模型调用与工具调用串起来。</p>
 */
@Slf4j
public class AgentTelemetry {

    public static final String METRIC_MODEL_CALL = "agent.model.call";
    public static final String METRIC_MODEL_TOKENS = "agent.model.tokens";
    public static final String METRIC_TOOL_CALL = "agent.tool.call";

    private static final String ATTR_START_NANOS = "agent.request.startNanos";

    private final MeterRegistry meterRegistry;
    private final AtomicLong modelCalls = new AtomicLong();
    private final AtomicLong modelErrors = new AtomicLong();
    private final AtomicLong toolCalls = new AtomicLong();
    private final AtomicLong toolErrors = new AtomicLong();

    public AgentTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 模型调用监听器：记录耗时、token 与异常。
     *
     * <p>开始时间放在请求上下文的 attributes 里，它会在同一次调用的请求/响应/异常回调间共享。</p>
     */
    public ChatModelListener chatModelListener() {
        return new ChatModelListener() {

            @Override
            public void onRequest(ChatModelRequestContext context) {
                context.attributes().put(ATTR_START_NANOS, System.nanoTime());
                if (log.isDebugEnabled()) {
                    log.debug("[{}] 模型调用开始 model={} 消息数={} 工具数={}",
                            TraceContext.current(),
                            context.chatRequest().modelName(),
                            context.chatRequest().messages() == null ? 0 : context.chatRequest().messages().size(),
                            context.chatRequest().toolSpecifications() == null ? 0 : context.chatRequest().toolSpecifications().size());
                }
            }

            @Override
            public void onResponse(ChatModelResponseContext context) {
                modelCalls.incrementAndGet();
                long elapsedNanos = elapsedNanos(context.attributes());
                recordModelCall(elapsedNanos, "success");

                TokenUsage usage = context.chatResponse() == null ? null : context.chatResponse().tokenUsage();
                recordTokens(usage);

                log.info("[{}] 模型调用完成 耗时={}ms 输入token={} 输出token={} 本轮请求工具={}",
                        TraceContext.current(),
                        elapsedNanos / 1_000_000,
                        usage == null ? "-" : usage.inputTokenCount(),
                        usage == null ? "-" : usage.outputTokenCount(),
                        requestedToolCount(context));
            }

            @Override
            public void onError(ChatModelErrorContext context) {
                modelCalls.incrementAndGet();
                modelErrors.incrementAndGet();
                long elapsedNanos = elapsedNanos(context.attributes());
                recordModelCall(elapsedNanos, "error");
                log.error("[{}] 模型调用失败 耗时={}ms 错误={}",
                        TraceContext.current(),
                        elapsedNanos / 1_000_000,
                        context.error() == null ? "unknown" : context.error().toString());
            }
        };
    }

    /**
     * 工具调用记录器，交给 AiServices 的 afterToolExecution 使用。
     */
    public Consumer<ToolExecution> toolExecutionRecorder() {
        return execution -> {
            boolean failed = execution.hasFailed();
            toolCalls.incrementAndGet();
            if (failed) {
                toolErrors.incrementAndGet();
            }

            String toolName = execution.request() == null ? "unknown" : execution.request().name();
            Duration duration = execution.duration() == null ? Duration.ZERO : execution.duration();
            Timer.builder(METRIC_TOOL_CALL)
                    .tag("tool", toolName)
                    .tag("outcome", failed ? "failed" : "ok")
                    .register(meterRegistry)
                    .record(duration);

            log.info("[{}] 工具调用完成 工具={} 耗时={}ms 结果={}",
                    TraceContext.current(), toolName, duration.toMillis(), failed ? "失败" : "成功");
        };
    }

    /**
     * 运行快照，供排查接口查看（不依赖 Prometheus 也能看到关键计数）。
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("modelCalls", modelCalls.get());
        snapshot.put("modelErrors", modelErrors.get());
        snapshot.put("toolCalls", toolCalls.get());
        snapshot.put("toolErrors", toolErrors.get());
        snapshot.put("modelCallSecondsCount", timerCount(METRIC_MODEL_CALL));
        snapshot.put("modelCallSecondsTotal", Math.round(timerTotalSeconds(METRIC_MODEL_CALL) * 1000) / 1000.0);
        snapshot.put("prometheusEndpoint", "/actuator/prometheus");
        return snapshot;
    }

    private void recordModelCall(long elapsedNanos, String outcome) {
        Timer.builder(METRIC_MODEL_CALL)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(elapsedNanos, 0)));
    }

    private void recordTokens(TokenUsage usage) {
        if (usage == null) {
            return;
        }
        if (usage.inputTokenCount() != null) {
            Counter.builder(METRIC_MODEL_TOKENS).tag("type", "input").register(meterRegistry)
                    .increment(usage.inputTokenCount());
        }
        if (usage.outputTokenCount() != null) {
            Counter.builder(METRIC_MODEL_TOKENS).tag("type", "output").register(meterRegistry)
                    .increment(usage.outputTokenCount());
        }
    }

    private int requestedToolCount(ChatModelResponseContext context) {
        if (context.chatResponse() == null || context.chatResponse().aiMessage() == null
                || context.chatResponse().aiMessage().toolExecutionRequests() == null) {
            return 0;
        }
        return context.chatResponse().aiMessage().toolExecutionRequests().size();
    }

    private long elapsedNanos(Map<Object, Object> attributes) {
        if (attributes == null) {
            return 0L;
        }
        Object start = attributes.get(ATTR_START_NANOS);
        if (!(start instanceof Long)) {
            return 0L;
        }
        return System.nanoTime() - (Long) start;
    }

    private long timerCount(String name) {
        return meterRegistry.find(name).timers().stream().mapToLong(Timer::count).sum();
    }

    private double timerTotalSeconds(String name) {
        return meterRegistry.find(name).timers().stream()
                .mapToDouble(timer -> timer.totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .sum();
    }
}
