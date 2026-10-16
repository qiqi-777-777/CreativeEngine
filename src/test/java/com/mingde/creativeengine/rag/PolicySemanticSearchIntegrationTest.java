package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实 Embedding 模型集成测试。
 *
 * <p>假模型只能验证索引逻辑，这里用随 jar 打包的 BGE small zh 真跑一遍，确认
 * 「模型能加载」+「语义召回能命中关键词匹配不到的政策」。代价是加载一次 ONNX 模型（数秒）。</p>
 */
class PolicySemanticSearchIntegrationTest {

    @Test
    void naturalLanguageQuestionFindsPolicyWithoutKeywordOverlap() {
        PolicyDataMapper policyDataMapper = mock(PolicyDataMapper.class);
        stubMapper(policyDataMapper, List.of(
                policy(1L, "高校毕业生一次性创业补贴", "高校毕业生、创业补贴",
                        "对毕业五年内的高校毕业生首次创办企业并正常经营满六个月的，给予一次性资金扶持。"),
                policy(2L, "创业孵化基地场地扶持", "场地、租金",
                        "入驻市级创业孵化基地的创业者，可享受办公场地租金减免。"),
                policy(3L, "小微企业社保返还", "社保、用工",
                        "小微企业招用登记失业人员并缴纳社会保险费的，可按实际缴费额给予返还。")));

        EmbeddingPolicyIndex index = new EmbeddingPolicyIndex(policyDataMapper, new BgeSmallZhV15EmbeddingModel(),
                DocumentSplitters.recursive(500, 50), 10, 0.0);
        index.afterPropertiesSet();

        // 问句与「高校毕业生」没有任何字面重叠，关键词检索无法命中
        List<PolicyData> hits = index.search("应届生自己开公司有什么钱可以领", 5);
        System.out.println("[RAG 冒烟] 问句=应届生自己开公司有什么钱可以领 召回顺序=" + ids(hits));

        assertFalse(hits.isEmpty(), "语义检索不应返回空结果");
        assertEquals(1L, hits.get(0).getId(), "应召回主题相同的高校毕业生创业扶持政策，实际顺序：" + ids(hits));
    }

    private static List<Long> ids(List<PolicyData> policies) {
        return policies.stream().map(PolicyData::getId).collect(Collectors.toList());
    }

    private static void stubMapper(PolicyDataMapper policyDataMapper, List<PolicyData> policies) {
        when(policyDataMapper.selectList(any())).thenReturn(policies);
        when(policyDataMapper.selectBatchIds(any())).thenAnswer(invocation -> {
            Collection<?> ids = invocation.getArgument(0);
            return policies.stream().filter(policy -> ids.contains(policy.getId())).collect(Collectors.toList());
        });
    }

    private static PolicyData policy(long id, String name, String keywords, String content) {
        PolicyData policy = new PolicyData();
        policy.setId(id);
        policy.setPolicyName(name);
        policy.setKeywords(keywords);
        policy.setContent(content);
        return policy;
    }
}
