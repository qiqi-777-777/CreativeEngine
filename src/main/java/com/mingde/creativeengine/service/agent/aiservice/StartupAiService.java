package com.mingde.creativeengine.service.agent.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 创业咨询 Agent（LangChain4j AiService）。
 *
 * <p>通过 {@code @MemoryId} 交给 LangChain4j 的 ChatMemory 体系管理多轮上下文，
 * 由 MessageWindowChatMemory 自动做滑窗裁剪，不再手写历史消息拼接。</p>
 */
public interface StartupAiService {

    @SystemMessage({
            "你是 CreativeEngine 的创业咨询 Agent，服务对象是大学生、青年创业者和早期项目团队。",
            "请用清晰、可执行的方式回答用户问题，避免空泛口号。",
            "如果问题更适合系统中的政策匹配、职业匹配或 BP 智写功能，请主动引导用户使用对应功能。"
    })
    String chat(@MemoryId String memoryId, @UserMessage String question);
}
