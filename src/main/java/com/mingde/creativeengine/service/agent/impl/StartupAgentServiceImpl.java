package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.service.agent.StartupAgentService;
import com.mingde.creativeengine.service.agent.aiservice.StartupAiService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 创业咨询 Agent 实现：多轮上下文交给 LangChain4j 的 ChatMemory 管理，
 * 由 MessageWindowChatMemory 自动做窗口裁剪；前端显式传入的历史记录
 * 只在会话记忆为空时用于初始化，避免覆盖服务端已有上下文。
 */
@Service
public class StartupAgentServiceImpl implements StartupAgentService {

    private static final Logger log = LoggerFactory.getLogger(StartupAgentServiceImpl.class);
    private static final int MAX_SEED_MESSAGES = 8;
    private static final String FAILURE_REPLY = "抱歉，AI 服务暂时不可用，请稍后再试。";

    private final StartupAiService startupAiService;
    private final ChatMemoryStore chatMemoryStore;

    public StartupAgentServiceImpl(StartupAiService startupAiService, ChatMemoryStore chatMemoryStore) {
        this.startupAiService = startupAiService;
        this.chatMemoryStore = chatMemoryStore;
    }

    @Override
    public String chat(String question) {
        return chatWithSession(newSessionId(), question, Collections.emptyList());
    }

    @Override
    public String chatWithHistory(String question, List<Map<String, Object>> history) {
        return chatWithSession(newSessionId(), question, history);
    }

    @Override
    public String chatWithSession(String sessionId, String question, List<Map<String, Object>> history) {
        if (question == null || question.trim().isEmpty()) {
            return "请先输入你想咨询的创业问题。";
        }

        String memoryId = (sessionId == null || sessionId.trim().isEmpty()) ? newSessionId() : sessionId.trim();

        try {
            seedMemoryFromHistory(memoryId, history);
            String answer = startupAiService.chat(memoryId, question.trim());
            if (answer == null || answer.trim().isEmpty()) {
                return "抱歉，我暂时无法回答这个问题。";
            }
            return answer;
        } catch (Exception e) {
            log.error("创业咨询 Agent 调用失败: {}", e.getMessage(), e);
            return FAILURE_REPLY;
        }
    }

    /**
     * 用请求携带的历史记录初始化会话记忆（仅在记忆为空时执行）。
     */
    private void seedMemoryFromHistory(String memoryId, List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        List<ChatMessage> existing = chatMemoryStore.getMessages(memoryId);
        if (existing != null && !existing.isEmpty()) {
            return;
        }

        List<ChatMessage> messages = new ArrayList<>();
        int startIndex = Math.max(0, history.size() - MAX_SEED_MESSAGES);
        for (int i = startIndex; i < history.size(); i++) {
            Map<String, Object> item = history.get(i);
            if (item == null) {
                continue;
            }
            Object role = item.get("role");
            Object content = item.get("content");
            if (role == null || content == null) {
                continue;
            }
            String text = content.toString().trim();
            if (text.isEmpty()) {
                continue;
            }
            if ("user".equals(role.toString())) {
                messages.add(UserMessage.from(text));
            } else if ("assistant".equals(role.toString())) {
                messages.add(AiMessage.from(text));
            }
        }

        if (!messages.isEmpty()) {
            chatMemoryStore.updateMessages(memoryId, messages);
        }
    }

    private String newSessionId() {
        return "startup-" + UUID.randomUUID();
    }
}
