package com.mingde.creativeengine.service.agent.impl;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.service.agent.PolicyAgentService;
import com.mingde.creativeengine.service.agent.aiservice.PolicyAiService;

import org.springframework.stereotype.Service;

/**
 * 政策解读 Agent 实现：查询政策业务数据后交给 LangChain4j 的
 * {@link PolicyAiService} 完成模型调用与结果解析。
 */
@Service
public class PolicyAgentServiceImpl implements PolicyAgentService {

    private static final int MAX_POLICY_CONTENT_LENGTH = 4000;

    private final PolicyDataService policyDataService;
    private final PolicyAiService policyAiService;

    public PolicyAgentServiceImpl(PolicyDataService policyDataService, PolicyAiService policyAiService) {
        this.policyDataService = policyDataService;
        this.policyAiService = policyAiService;
    }

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

        return policyAiService.askByPolicy(
                defaultValue(policy.getPolicyName()),
                defaultValue(policy.getKeywords()),
                truncate(defaultValue(policy.getContent()), MAX_POLICY_CONTENT_LENGTH),
                question.trim()
        );
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
