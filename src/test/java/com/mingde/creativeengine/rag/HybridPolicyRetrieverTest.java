package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.PolicyDataService;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 混合召回测试：重点是 RRF 融合的排序是否符合预期，以及两路缺失时的降级行为。
 */
class HybridPolicyRetrieverTest {

    private static final int CANDIDATE_SIZE = 10;

    private final PolicyDataService policyDataService = mock(PolicyDataService.class);
    private final SemanticPolicySearch semanticSearch = mock(SemanticPolicySearch.class);
    private final HybridPolicyRetriever retriever =
            new HybridPolicyRetriever(policyDataService, semanticSearch, CANDIDATE_SIZE);

    @Test
    void policyHitByBothSourcesOutranksSingleSourceHits() {
        // 2 号被语义与关键词各命中一次（各自排第二）
        when(semanticSearch.search(anyString(), anyInt())).thenReturn(List.of(policy(1L), policy(2L)));
        when(policyDataService.searchPolicies(anyString())).thenReturn(List.of(policy(3L), policy(2L)));

        List<PolicyData> hits = retriever.retrieve("应届生能领什么补贴", 5);

        assertEquals(List.of(2L, 1L, 3L), ids(hits), "两路都命中的应排在只有单路命中的前面");
    }

    @Test
    void keepsKeywordOnlyHitsWhenSemanticReturnsNothing() {
        when(semanticSearch.search(anyString(), anyInt())).thenReturn(List.of());
        when(policyDataService.searchPolicies(anyString())).thenReturn(List.of(policy(7L), policy(8L)));

        assertEquals(List.of(7L, 8L), ids(retriever.retrieve("税收减免", 5)));
    }

    @Test
    void blankQuerySkipsSemanticSearch() {
        when(policyDataService.searchPolicies("")).thenReturn(List.of(policy(1L), policy(2L)));

        assertEquals(List.of(1L, 2L), ids(retriever.retrieve("   ", 5)));
        verify(semanticSearch, never()).search(anyString(), anyInt());
    }

    @Test
    void respectsMaxResults() {
        when(semanticSearch.search(anyString(), anyInt())).thenReturn(List.of(policy(1L), policy(2L), policy(3L)));
        when(policyDataService.searchPolicies(anyString())).thenReturn(List.of(policy(4L), policy(5L), policy(6L)));

        assertEquals(2, retriever.retrieve("补贴", 2).size());
    }

    @Test
    void ignoresHitsWithoutId() {
        PolicyData noId = new PolicyData();
        when(semanticSearch.search(anyString(), anyInt())).thenReturn(List.of(policy(1L), noId));
        when(policyDataService.searchPolicies(anyString())).thenReturn(List.of());

        assertEquals(List.of(1L), ids(retriever.retrieve("补贴", 5)));
    }

    private static List<Long> ids(List<PolicyData> policies) {
        return policies.stream().map(PolicyData::getId).toList();
    }

    private static PolicyData policy(long id) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        return policy;
    }
}
