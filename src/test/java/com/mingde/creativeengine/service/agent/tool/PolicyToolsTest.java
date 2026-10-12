package com.mingde.creativeengine.service.agent.tool;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.PolicyDataService;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 政策工具测试：工具返回值直接进入模型上下文，这里固定住它的几个关键契约。
 */
class PolicyToolsTest {

    private static class StubPolicyDataService implements PolicyDataService {

        private List<PolicyData> searchResult = Collections.emptyList();
        private PolicyData byId;
        private RuntimeException failure;
        private String lastKeyword;

        @Override
        public List<PolicyData> getAllPolicies() {
            return Collections.emptyList();
        }

        @Override
        public PolicyData getPolicyById(Long id) {
            if (failure != null) {
                throw failure;
            }
            return byId;
        }

        @Override
        public PolicyData getPolicyByName(String policyName) {
            return null;
        }

        @Override
        public List<PolicyData> searchPolicies(String keyword) {
            lastKeyword = keyword;
            if (failure != null) {
                throw failure;
            }
            return searchResult;
        }
    }

    private static PolicyData policy(long id, String name, String keywords, String content) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        policy.setPolicyName(name);
        policy.setKeywords(keywords);
        policy.setContent(content);
        return policy;
    }

    @Test
    void searchResultExposesPolicyIdSoModelCanFetchDetailNext() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.searchResult = List.of(
                policy(9L, "大学生创业补贴政策", "补贴、创业", "正文A"),
                policy(10L, "税费减免政策", "税收", "正文B"));

        String result = new PolicyTools(service).searchPolicies("创业补贴");

        assertTrue(result.contains("[政策ID=9]"), result);
        assertTrue(result.contains("大学生创业补贴政策"), result);
        assertTrue(result.contains("getPolicyDetail"), "应提示模型下一步取正文：" + result);
        assertTrue(result.contains("共检索到 2 条"), result);
    }

    @Test
    void emptySearchTellsModelNothingFoundInsteadOfReturningBlank() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.searchResult = Collections.emptyList();

        String result = new PolicyTools(service).searchPolicies("火星移民补贴");

        assertTrue(result.contains("没有检索到"), result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void searchFailureIsReturnedAsReadableTextNotThrown() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.failure = new IllegalStateException("connection refused");

        String result = assertDoesNotThrow(() -> new PolicyTools(service).searchPolicies("补贴"));

        assertTrue(result.contains("暂时不可用"), result);
        assertTrue(result.contains("官方文件"), "应指导模型改为保守回答：" + result);
        assertFalse(result.contains("connection refused"), "不应把底层异常细节抛给模型：" + result);
    }

    @Test
    void detailTruncatesOverlongContent() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.byId = policy(9L, "超长政策", "补贴", "内".repeat(5000));

        String result = new PolicyTools(service).getPolicyDetail("9");

        assertTrue(result.contains("标题：超长政策"), result);
        assertTrue(result.contains("已截断"), "超长正文应截断：" + result);
        assertTrue(result.length() < 2200, "返回值应控制在 2000 字左右，实际 " + result.length());
    }

    @Test
    void invalidPolicyIdReturnsHintWithoutThrowing() {
        StubPolicyDataService service = new StubPolicyDataService();

        String result = assertDoesNotThrow(() -> new PolicyTools(service).getPolicyDetail("九号政策"));

        assertTrue(result.contains("无效"), result);
        assertTrue(result.contains("searchPolicies"), result);
    }

    @Test
    void missingPolicyReturnsHintWithoutThrowing() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.byId = null;

        String result = new PolicyTools(service).getPolicyDetail("999");

        assertTrue(result.contains("没有找到"), result);
    }
}
