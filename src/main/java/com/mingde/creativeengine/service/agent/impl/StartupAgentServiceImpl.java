package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.agent.StartupAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Startup consulting agent based on DeepSeek.
 */
@Service
public class StartupAgentServiceImpl implements StartupAgentService {

    private static final int MAX_HISTORY_SIZE = 8;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public String chat(String question) {
        return chatWithHistory(question, Collections.emptyList());
    }

    @Override
    public String chatWithHistory(String question, List<Map<String, Object>> history) {
        if (question == null || question.trim().isEmpty()) {
            return "请先输入你想咨询的创业问题。";
        }

        String prompt = buildPrompt(question, history);
        return deepSeekService.chat(prompt);
    }

    private String buildPrompt(String question, List<Map<String, Object>> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 CreativeEngine 的创业咨询 Agent，服务对象是大学生、青年创业者和早期项目团队。\n");
        prompt.append("请用清晰、可执行的方式回答用户问题，避免空泛口号。\n");
        prompt.append("如果问题更适合系统中的政策匹配、职业匹配或 BP 智写功能，请主动引导用户使用对应功能。\n\n");

        appendHistory(prompt, history);

        prompt.append("用户当前问题：").append(question.trim()).append("\n\n");
        prompt.append("请按照以下结构回答：\n");
        prompt.append("1. 直接回答\n");
        prompt.append("2. 具体行动步骤\n");
        prompt.append("3. 可使用的系统功能\n");
        prompt.append("4. 后续建议\n");
        return prompt.toString();
    }

    private void appendHistory(StringBuilder prompt, List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        prompt.append("最近对话历史：\n");
        int startIndex = Math.max(0, history.size() - MAX_HISTORY_SIZE);
        for (int i = startIndex; i < history.size(); i++) {
            Map<String, Object> item = history.get(i);
            Object role = item.get("role");
            Object content = item.get("content");
            if (role == null || content == null || content.toString().trim().isEmpty()) {
                continue;
            }
            prompt.append(role).append("：").append(content.toString().trim()).append("\n");
        }
        prompt.append("\n");
    }
}
