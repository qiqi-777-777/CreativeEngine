package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.PolicyDataService;

import java.util.List;

/**
 * 未开启 RAG 时的实现：沿用原有的关键词（LIKE）检索。
 *
 * <p>保证本地零依赖启动时政策检索的行为与改造前完全一致。</p>
 */
public class KeywordPolicyRetriever implements PolicyRetriever {

    private final PolicyDataService policyDataService;

    public KeywordPolicyRetriever(PolicyDataService policyDataService) {
        this.policyDataService = policyDataService;
    }

    @Override
    public List<PolicyData> retrieve(String query, int maxResults) {
        List<PolicyData> policies = policyDataService.searchPolicies(query);
        if (policies == null) {
            return List.of();
        }
        return policies.size() <= maxResults ? policies : policies.subList(0, maxResults);
    }
}
