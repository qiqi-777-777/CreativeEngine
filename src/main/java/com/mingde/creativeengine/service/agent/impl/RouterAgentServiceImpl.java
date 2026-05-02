package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.service.agent.BpAgentService;
import com.mingde.creativeengine.service.agent.CareerAgentService;
import com.mingde.creativeengine.service.agent.PolicyAgentService;
import com.mingde.creativeengine.service.agent.RouterAgentService;
import com.mingde.creativeengine.service.agent.StartupAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-first router agent. It keeps routing fast and avoids an extra LLM call
 * before the selected scene agent handles the real task.
 */
@Service
public class RouterAgentServiceImpl implements RouterAgentService {

    private static final int MAX_POLICY_COUNT = 3;
    private static final int MAX_POLICY_CONTENT_LENGTH = 900;

    private static final List<String> POLICY_KEYWORDS = Arrays.asList(
            "政策", "补贴", "扶持", "申报", "申请", "税收", "减免", "资助", "贷款", "创业补助", "大学生创业"
    );

    private static final List<String> CAREER_KEYWORDS = Arrays.asList(
            "职业", "岗位", "就业", "专业", "技能", "性格", "匹配", "职业规划", "发展方向", "工作"
    );

    private static final List<String> BP_KEYWORDS = Arrays.asList(
            "bp", "BP", "商业计划书", "计划书", "项目书", "融资", "路演", "商业模式", "竞品", "痛点"
    );

    @Autowired
    private PolicyAgentService policyAgentService;

    @Autowired
    private CareerAgentService careerAgentService;

    @Autowired
    private BpAgentService bpAgentService;

    @Autowired
    private StartupAgentService startupAgentService;

    @Autowired
    private PolicyDataService policyDataService;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public Map<String, Object> route(String question, Map<String, Object> context) {
        Map<String, Object> safeContext = context == null ? new HashMap<>() : context;
        String route = detectRoute(question, safeContext);
        String answer;

        switch (route) {
            case "policy":
                answer = handlePolicy(question, safeContext);
                break;
            case "career":
                answer = handleCareer(question, safeContext);
                break;
            case "bp":
                answer = handleBp(question, safeContext);
                break;
            case "startup":
            default:
                answer = startupAgentService.chatWithHistory(question, getHistory(safeContext));
                route = "startup";
                break;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("route", route);
        result.put("answer", answer);
        result.put("response", answer);
        result.put("reason", routeReason(route));
        return result;
    }

    private String detectRoute(String question, Map<String, Object> context) {
        String explicitRoute = normalizeRoute(getString(context, "route"));
        if (!explicitRoute.isEmpty()) {
            return explicitRoute;
        }

        explicitRoute = normalizeRoute(getString(context, "agentType"));
        if (!explicitRoute.isEmpty()) {
            return explicitRoute;
        }

        if (getLong(context, "policyId") != null) {
            return "policy";
        }
        if (hasAnyList(context, "majors", "skills", "personalities")) {
            return "career";
        }
        if (hasAnyText(context, "projectName", "oneLiner", "painPoint", "painPoints", "solution", "businessModel")) {
            return "bp";
        }

        String text = question == null ? "" : question;
        if (containsAny(text, BP_KEYWORDS)) {
            return "bp";
        }
        if (containsAny(text, POLICY_KEYWORDS)) {
            return "policy";
        }
        if (containsAny(text, CAREER_KEYWORDS)) {
            return "career";
        }
        return "startup";
    }

    private String normalizeRoute(String route) {
        if (route == null) {
            return "";
        }
        String normalized = route.trim().toLowerCase();
        if ("policy".equals(normalized) || "career".equals(normalized)
                || "bp".equals(normalized) || "startup".equals(normalized)) {
            return normalized;
        }
        return "";
    }

    private String handlePolicy(String question, Map<String, Object> context) {
        Long policyId = getLong(context, "policyId");
        if (policyId != null) {
            return policyAgentService.askPolicy(policyId, question);
        }

        List<PolicyData> policies = policyDataService.searchPolicies(question);
        if (policies == null || policies.isEmpty()) {
            return startupAgentService.chat("用户正在咨询政策问题，但数据库中没有检索到相关政策。请回答：" + question);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个面向大学生和青年创业者的政策解读 Agent。\n");
        prompt.append("请基于以下政策数据库检索结果回答用户问题，不要编造材料中不存在的政策细节。\n");
        prompt.append("如果材料不足，请说明需要继续查看官方文件或主管部门通知。\n\n");
        prompt.append("用户问题：").append(question).append("\n\n");
        prompt.append("政策检索结果：\n");

        for (int i = 0; i < Math.min(MAX_POLICY_COUNT, policies.size()); i++) {
            PolicyData policy = policies.get(i);
            prompt.append(i + 1).append(". 标题：").append(defaultValue(policy.getPolicyName())).append("\n");
            prompt.append("关键词：").append(defaultValue(policy.getKeywords())).append("\n");
            prompt.append("内容摘要：").append(truncate(defaultValue(policy.getContent()), MAX_POLICY_CONTENT_LENGTH)).append("\n\n");
        }

        prompt.append("请输出：\n");
        prompt.append("1. 相关政策方向\n");
        prompt.append("2. 用户可能关注的条件\n");
        prompt.append("3. 申报或查询建议\n");
        prompt.append("4. 风险提示\n");
        return deepSeekService.chat(prompt.toString());
    }

    private String handleCareer(String question, Map<String, Object> context) {
        List<String> majors = getStringList(context, "majors");
        List<String> skills = getStringList(context, "skills");
        List<String> personalities = getStringList(context, "personalities");

        if (!majors.isEmpty() || !skills.isEmpty() || !personalities.isEmpty()) {
            return careerAgentService.suggestCareer(majors, skills, personalities);
        }

        return startupAgentService.chat("请以职业规划顾问身份回答以下问题，并给出适合大学生的可执行建议：" + question);
    }

    private String handleBp(String question, Map<String, Object> context) {
        Map<String, Object> bpInfo = new HashMap<>(context);
        bpInfo.put("question", question);

        if (hasAnyText(bpInfo, "projectName", "oneLiner", "painPoint", "painPoints", "solution", "businessModel")) {
            return bpAgentService.generateBp(bpInfo);
        }

        return startupAgentService.chat("请以商业计划书写作顾问身份回答以下问题：" + question);
    }

    private String routeReason(String route) {
        switch (route) {
            case "policy":
                return "识别为政策咨询场景，已交给政策解读 Agent 处理";
            case "career":
                return "识别为职业规划场景，已交给职业规划 Agent 处理";
            case "bp":
                return "识别为 BP 写作场景，已交给 BP 写作 Agent 处理";
            case "startup":
            default:
                return "识别为通用创业咨询场景，已交给创业咨询 Agent 处理";
        }
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyText(Map<String, Object> context, String... keys) {
        for (String key : keys) {
            if (!getString(context, key).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyList(Map<String, Object> context, String... keys) {
        for (String key : keys) {
            if (!getStringList(context, key).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String getString(Map<String, Object> context, String key) {
        Object value = context.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private Long getLong(Map<String, Object> context, String key) {
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> getStringList(Map<String, Object> context, String key) {
        Object value = context.get(key);
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }

        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    result.add(item.toString().trim());
                }
            }
            return result;
        }

        for (String item : value.toString().split("[,，、;；\\s]+")) {
            if (!item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHistory(Map<String, Object> context) {
        Object value = context.get("history");
        List<Map<String, Object>> history = new ArrayList<>();
        if (!(value instanceof List<?>)) {
            return history;
        }

        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                history.add((Map<String, Object>) item);
            }
        }
        return history;
    }

    private String defaultValue(String value) {
        return value == null || value.trim().isEmpty() ? "未提供" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n[内容过长，已截取前 " + maxLength + " 字]";
    }
}
