package com.mingde.creativeengine.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.ai.OccupationRecommendation;
import com.mingde.creativeengine.service.agent.aiservice.OccupationAiService;

import dev.langchain4j.model.chat.ChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 大模型服务实现（基于 LangChain4j）。
 *
 * <p>模型调用、超时、重试与响应解析由 {@link ChatModel} 统一处理；
 * 职业推荐走 DeepSeek 的 json_object 模式，返回的 JSON 在这里解析为对象，
 * 任何异常都会降级为兜底内容，保证业务功能不中断。</p>
 */
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekServiceImpl.class);

    private static final String BUSY_REPLY = "抱歉，AI服务暂时繁忙，请稍后再试。";
    private static final String EMPTY_REPLY = "抱歉，我暂时无法回答这个问题。";
    private static final String RECOMMENDATIONS_FIELD = "recommendations";

    private final ChatModel chatModel;
    private final OccupationAiService occupationAiService;
    private final ObjectMapper objectMapper;

    public DeepSeekServiceImpl(ChatModel chatModel,
                               OccupationAiService occupationAiService,
                               ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.occupationAiService = occupationAiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "请输入你想咨询的问题。";
        }

        try {
            String content = chatModel.chat(prompt);
            if (content == null || content.trim().isEmpty()) {
                log.warn("大模型返回空内容");
                return EMPTY_REPLY;
            }
            return content;
        } catch (Exception e) {
            log.error("调用大模型失败: {}", e.getMessage(), e);
            return BUSY_REPLY;
        }
    }

    @Override
    public List<Map<String, String>> generateOccupationRecommendations(
            List<String> majors,
            List<String> skills,
            List<String> personalities,
            int count
    ) {
        int safeCount = Math.max(1, count);
        try {
            String json = occupationAiService.recommendJson(
                    formatList(majors),
                    formatList(skills),
                    formatList(personalities),
                    safeCount
            );

            List<OccupationRecommendation> recommendations = parseRecommendations(json);
            if (recommendations.isEmpty()) {
                return getDefaultRecommendations(safeCount);
            }

            return recommendations.stream()
                    .filter(item -> item != null && item.getName() != null && !item.getName().trim().isEmpty())
                    .limit(safeCount)
                    .map(this::toMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("生成职业推荐失败: {}", e.getMessage(), e);
            return getDefaultRecommendations(safeCount);
        }
    }

    /**
     * 兼容两种返回结构：直接是数组，或被包在 recommendations 字段里。
     */
    private List<OccupationRecommendation> parseRecommendations(String json) throws Exception {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode root = objectMapper.readTree(stripCodeFence(json));
        JsonNode arrayNode = root.isArray() ? root : root.path(RECOMMENDATIONS_FIELD);
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(arrayNode, new TypeReference<List<OccupationRecommendation>>() {
        });
    }

    /**
     * 部分模型会把 JSON 包在 markdown 代码块里，这里做一次兼容。
     */
    private String stripCodeFence(String text) {
        String cleaned = text.trim();
        if (!cleaned.startsWith("```")) {
            return cleaned;
        }
        int firstLineBreak = cleaned.indexOf('\n');
        if (firstLineBreak > 0) {
            cleaned = cleaned.substring(firstLineBreak + 1);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private Map<String, String> toMap(OccupationRecommendation item) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("name", item.getName().trim());
        result.put("reason", item.getReason() == null ? "" : item.getReason().trim());
        return result;
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "未提供";
        }
        String text = values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("、"));
        return text.isEmpty() ? "未提供" : text;
    }

    /**
     * 模型不可用时返回的兜底推荐，保证职业匹配页面始终有内容可展示。
     */
    private List<Map<String, String>> getDefaultRecommendations(int count) {
        List<Map<String, String>> defaults = new ArrayList<>();

        Map<String, String> first = new LinkedHashMap<>();
        first.put("name", "产品经理");
        first.put("reason", "综合技能要求高，适合多元化背景人才");
        defaults.add(first);

        Map<String, String> second = new LinkedHashMap<>();
        second.put("name", "项目经理");
        second.put("reason", "注重沟通协调，适合组织能力强的人");
        defaults.add(second);

        Map<String, String> third = new LinkedHashMap<>();
        third.put("name", "业务分析师");
        third.put("reason", "连接技术与业务，发展前景广阔");
        defaults.add(third);

        return Collections.unmodifiableList(defaults.subList(0, Math.min(count, defaults.size())));
    }
}
