package com.mingde.creativeengine.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutionResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可观测性测试：直接驱动监听器回调，验证指标确实被记录。
 *
 * <p>不依赖真实模型与网络，只断言"模型调了多少次、花多少 token、工具成功还是失败"这些
 * 信息进入了 Micrometer，避免改坏监听器却没有任何测试兜底。</p>
 */
class AgentTelemetryTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AgentTelemetry telemetry = new AgentTelemetry(meterRegistry);

    @Test
    void recordsModelLatencyAndTokensOnResponse() {
        ChatModelListener listener = telemetry.chatModelListener();
        Map<Object, Object> attributes = new HashMap<>();
        ChatRequest request = request();

        listener.onRequest(new ChatModelRequestContext(request, ModelProvider.OPEN_AI, attributes));
        listener.onResponse(new ChatModelResponseContext(
                ChatResponse.builder()
                        .aiMessage(AiMessage.from("好的"))
                        .tokenUsage(new TokenUsage(120, 30))
                        .build(),
                request,
                ModelProvider.OPEN_AI,
                attributes));

        assertEquals(1.0, timerCount("agent.model.call", "outcome", "success"), 0.0001,
                "模型调用成功应记录一次耗时指标");
        assertEquals(120.0, counterValue("agent.model.tokens", "type", "input"), 0.0001);
        assertEquals(30.0, counterValue("agent.model.tokens", "type", "output"), 0.0001);

        Map<String, Object> snapshot = telemetry.snapshot();
        assertEquals(1L, snapshot.get("modelCalls"));
        assertEquals(0L, snapshot.get("modelErrors"));
        assertNotNull(snapshot.get("prometheusEndpoint"));
    }

    @Test
    void recordsModelErrorSeparatelyFromSuccess() {
        ChatModelListener listener = telemetry.chatModelListener();
        Map<Object, Object> attributes = new HashMap<>();
        ChatRequest request = request();

        listener.onRequest(new ChatModelRequestContext(request, ModelProvider.OPEN_AI, attributes));
        listener.onError(new ChatModelErrorContext(
                new IllegalStateException("boom"), request, ModelProvider.OPEN_AI, attributes));

        assertEquals(1.0, timerCount("agent.model.call", "outcome", "error"), 0.0001,
                "模型调用失败应按 error 打标签记录");
        assertEquals(0.0, timerCount("agent.model.call", "outcome", "success"), 0.0001);

        Map<String, Object> snapshot = telemetry.snapshot();
        assertEquals(1L, snapshot.get("modelErrors"));
    }

    @Test
    void recordsToolCallOutcome() {
        Consumer<ToolExecution> recorder = telemetry.toolExecutionRecorder();

        recorder.accept(toolExecution("searchPolicies", false));
        recorder.accept(toolExecution("searchPolicies", true));

        assertEquals(1.0, timerCount("agent.tool.call", "outcome", "ok"), 0.0001);
        assertEquals(1.0, timerCount("agent.tool.call", "outcome", "failed"), 0.0001);

        Map<String, Object> snapshot = telemetry.snapshot();
        assertEquals(2L, snapshot.get("toolCalls"));
        assertEquals(1L, snapshot.get("toolErrors"));
    }

    private static ChatRequest request() {
        return ChatRequest.builder()
                .modelName("deepseek-chat")
                .messages(List.of(UserMessage.from("你好")))
                .toolSpecifications(List.of())
                .build();
    }

    private static ToolExecution toolExecution(String toolName, boolean failed) {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1")
                .name(toolName)
                .arguments("{}")
                .build();
        LocalDateTime start = LocalDateTime.now();
        return ToolExecution.builder()
                .request(request)
                .result(ToolExecutionResult.builder()
                        .isError(failed)
                        .resultText(failed ? "工具执行失败" : "工具执行结果")
                        .build())
                .startTime(start)
                .finishTime(start.plusNanos(5_000_000L))
                .invocationContext(invocationContext())
                .build();
    }

    private static InvocationContext invocationContext() {
        return InvocationContext.builder()
                .invocationId(UUID.randomUUID())
                .interfaceName("test.ToolInterface")
                .methodName("tool")
                .methodArguments(List.of())
                .chatMemoryId("test-memory")
                .invocationParameters(new InvocationParameters())
                .timestampNow()
                .build();
    }

    private double timerCount(String name, String tagKey, String tagValue) {
        return meterRegistry.find(name).tag(tagKey, tagValue).timers().stream()
                .mapToDouble(timer -> timer.count())
                .sum();
    }

    private double counterValue(String name, String tagKey, String tagValue) {
        double value = meterRegistry.find(name).tag(tagKey, tagValue).counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
        assertTrue(value > 0, "指标 " + name + "[" + tagKey + "=" + tagValue + "] 没有被记录");
        return value;
    }
}
