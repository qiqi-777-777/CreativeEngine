package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.DeepSeekService;
import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.service.agent.PolicyAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Policy agent based on stored policy data and DeepSeek.
 */
@Service
public class PolicyAgentServiceImpl implements PolicyAgentService {

    private static final int MAX_POLICY_CONTENT_LENGTH = 4000;

    @Autowired
    private PolicyDataService policyDataService;

    @Autowired
    private DeepSeekService deepSeekService;

    @Override
    public String askPolicy(Long policyId, String question) {
        if (policyId == null) {
            return "请先选择需要解读的政策。";
        }
        if (isBlank(question)) {
            return "请先输入你想咨询的政策问题。";
        }

        PolicyData policy = policyDataService.getPolicyById(policyId);
        if (policy == null) {
            return "未找到对应政策，请确认政策是否存在。";
        }

        String prompt = buildPrompt(policy, question);
        return deepSeekService.chat(prompt);
    }

    private String buildPrompt(PolicyData policy, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个面向大学生和青年创业者的政策解读助手。\n");
        prompt.append("请严格基于给定政策内容回答用户问题，不要编造政策中不存在的信息。\n");
        prompt.append("如果政策内容无法支持结论，请明确说明需要以官方文件或主管部门解释为准。\n\n");
        prompt.append("政策标题：").append(defaultValue(policy.getPolicyName())).append("\n");
        prompt.append("政策关键词：").append(defaultValue(policy.getKeywords())).append("\n");
        prompt.append("政策内容：").append(truncate(defaultValue(policy.getContent()), MAX_POLICY_CONTENT_LENGTH)).append("\n\n");
        prompt.append("用户问题：").append(question.trim()).append("\n\n");
        prompt.append("请按照以下结构回答：\n");
        prompt.append("1. 政策含义\n");
        prompt.append("2. 适用对象\n");
        prompt.append("3. 用户可能匹配的条件\n");
        prompt.append("4. 申报建议\n");
        prompt.append("5. 注意事项\n");
        return prompt.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
