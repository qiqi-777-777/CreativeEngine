package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * AiService 工厂：集中构建各场景 Agent 的 LangChain4j 代理。
 *
 * <p>Spring 配置类和单元测试共用同一套构建逻辑，避免两处装配方式不一致。</p>
 */
public final class AgentAiServiceFactory {

    /** 创业咨询 Agent 保留的对话窗口条数。 */
    public static final int STARTUP_MEMORY_MAX_MESSAGES = 8;

    private AgentAiServiceFactory() {
    }

    public static PolicyAiService policyAiService(ChatModel chatModel) {
        return AiServices.builder(PolicyAiService.class)
                .chatModel(chatModel)
                .build();
    }

    public static CareerAiService careerAiService(ChatModel chatModel) {
        return AiServices.builder(CareerAiService.class)
                .chatModel(chatModel)
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
}
