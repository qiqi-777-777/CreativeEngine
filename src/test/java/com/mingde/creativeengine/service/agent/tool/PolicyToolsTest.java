package com.mingde.creativeengine.service.agent.tool;

import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.rag.KeywordPolicyRetriever;
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

        @Override
        public PageResult<PolicyData> pagePolicies(String keyword, int page, int size) {
            return PageResult.of(searchPolicies(keyword), searchResult.size(), page, size);
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

    /** 未开启 RAG 时的检索链路：关键词直通，与改造前行为一致。 */
    private static PolicyTools tools(StubPolicyDataService service) {
        return new PolicyTools(service, new KeywordPolicyRetriever(service));
    }

    @Test
    void searchResultExposesPolicyIdSoModelCanFetchDetailNext() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.searchResult = List.of(
                policy(9L, "大学生创业补贴政策", "补贴、创业", "正文A"),
                policy(10L, "税费减免政策", "税收", "正文B"));

        String result = tools(service).searchPolicies("创业补贴");

        assertTrue(result.contains("[政策ID=9]"), result);
        assertTrue(result.contains("大学生创业补贴政策"), result);
        assertTrue(result.contains("getPolicyDetail"), "应提示模型下一步取正文：" + result);
        assertTrue(result.contains("检索到 2 条政策"), result);
    }

    @Test
    void searchResultIsCappedAtFiveEntries() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.searchResult = List.of(
                policy(1L, "政策1", "补贴", "正文"),
                policy(2L, "政策2", "补贴", "正文"),
                policy(3L, "政策3", "补贴", "正文"),
                policy(4L, "政策4", "补贴", "正文"),
                policy(5L, "政策5", "补贴", "正文"),
                policy(6L, "政策6", "补贴", "正文"));

        String result = tools(service).searchPolicies("补贴");

        assertTrue(result.contains("[政策ID=5]"), result);
        assertFalse(result.contains("[政策ID=6]"), "最多只应给出 5 条，避免灌满上下文：" + result);
    }

    @Test
    void emptySearchTellsModelNothingFoundInsteadOfReturningBlank() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.searchResult = Collections.emptyList();

        String result = tools(service).searchPolicies("火星移民补贴");

        assertTrue(result.contains("没有检索到"), result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void searchFailureIsReturnedAsReadableTextNotThrown() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.failure = new IllegalStateException("connection refused");

        String result = assertDoesNotThrow(() -> tools(service).searchPolicies("补贴"));

        assertTrue(result.contains("暂时不可用"), result);
        assertTrue(result.contains("官方文件"), "应指导模型改为保守回答：" + result);
        assertFalse(result.contains("connection refused"), "不应把底层异常细节抛给模型：" + result);
    }

    @Test
    void detailTruncatesOverlongContent() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.byId = policy(9L, "超长政策", "补贴", "内".repeat(5000));

        String result = tools(service).getPolicyDetail("9");

        assertTrue(result.contains("标题：超长政策"), result);
        assertTrue(result.contains("已截断"), "超长正文应截断：" + result);
        assertTrue(result.length() < 2200, "返回值应控制在 2000 字左右，实际 " + result.length());
    }

    @Test
    void invalidPolicyIdReturnsHintWithoutThrowing() {
        StubPolicyDataService service = new StubPolicyDataService();

        String result = assertDoesNotThrow(() -> tools(service).getPolicyDetail("九号政策"));

        assertTrue(result.contains("无效"), result);
        assertTrue(result.contains("searchPolicies"), result);
    }

    @Test
    void missingPolicyReturnsHintWithoutThrowing() {
        StubPolicyDataService service = new StubPolicyDataService();
        service.byId = null;

        String result = tools(service).getPolicyDetail("999");

        assertTrue(result.contains("没有找到"), result);
    }
}
