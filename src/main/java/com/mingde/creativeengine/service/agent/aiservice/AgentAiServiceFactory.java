package com.mingde.creativeengine.service.agent.aiservice;

import com.mingde.creativeengine.observability.AgentTelemetry;
import com.mingde.creativeengine.service.agent.tool.OccupationTools;
import com.mingde.creativeengine.service.agent.tool.PolicyTools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.function.Function;

/**
 * AiService 工厂：集中构建各场景 Agent 的 LangChain4j 代理。
 *
 * <p>Spring 配置类和单元测试共用同一套构建逻辑，避免两处装配方式不一致。</p>
 */
public final class AgentAiServiceFactory {

    /** 创业咨询 Agent 保留的对话窗口条数。 */
    public static final int STARTUP_MEMORY_MAX_MESSAGES = 8;

    /**
     * 单次问答内最多允许的模型—工具往返轮次。
     *
     * <p>不设上限的话，模型有可能在两个工具之间来回调用（查了正文又去检索、检索完又查正文），
     * 既浪费时间也浪费 token，这里给一个硬上限。</p>
     */
    public static final int MAX_TOOL_CALLING_ROUND_TRIPS = 5;

    private AgentAiServiceFactory() {
    }

    /**
     * 政策解读 Agent：注册政策检索与正文查询工具，由模型决定检索关键词与要解读哪一条。
     */
    public static PolicyAiService policyAiService(ChatModel chatModel, PolicyTools policyTools, AgentTelemetry telemetry) {
        return AiServices.builder(PolicyAiService.class)
                .chatModel(chatModel)
                .tools(policyTools)
                .maxToolCallingRoundTrips(MAX_TOOL_CALLING_ROUND_TRIPS)
                .hallucinatedToolNameStrategy(hallucinatedToolStrategy("searchPolicies", "getPolicyDetail"))
                .afterToolExecution(telemetry.toolExecutionRecorder())
                .build();
    }

    /**
     * 职业规划 Agent：注册职业标签与匹配工具。
     */
    public static CareerAiService careerAiService(ChatModel chatModel, OccupationTools occupationTools, AgentTelemetry telemetry) {
        return AiServices.builder(CareerAiService.class)
                .chatModel(chatModel)
                .tools(occupationTools)
                .maxToolCallingRoundTrips(MAX_TOOL_CALLING_ROUND_TRIPS)
                .hallucinatedToolNameStrategy(hallucinatedToolStrategy("getOccupationTags", "matchOccupations"))
                .afterToolExecution(telemetry.toolExecutionRecorder())
                .build();
    }

    public static BpAiService bpAiService(ChatModel chatModel) {
        return AiServices.builder(BpAiService.class)
                .chatModel(chatModel)
                .build();
    }

    public static OccupationAiService occupationAiService(ChatModel chatModel) {
        return AiServices.builder(OccupationAiService.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 创业咨询 Agent 需要多轮上下文，按 memoryId 分配 ChatMemory，
     * 并统一落到共享的 ChatMemoryStore 上，保证同一会话可以跨请求复用。
     */
    public static StartupAiService startupAiService(ChatModel chatModel, ChatMemoryStore chatMemoryStore) {
        return AiServices.builder(StartupAiService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(STARTUP_MEMORY_MAX_MESSAGES)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }

    /**
     * 模型幻觉出一个不存在的工具名时，默认行为会抛异常直接中断整次问答。
     * 这里改成把"工具不存在 + 当前可用工具"作为工具结果回灌给模型，让它改用正确工具继续推理。
     */
    private static Function<ToolExecutionRequest, ToolExecutionResultMessage> hallucinatedToolStrategy(String... toolNames) {
        String available = String.join("、", toolNames);
        return request -> ToolExecutionResultMessage.from(request,
                "工具「" + request.name() + "」不存在。当前可用的工具是：" + available + "，请改用可用工具重试。");
    }
}
