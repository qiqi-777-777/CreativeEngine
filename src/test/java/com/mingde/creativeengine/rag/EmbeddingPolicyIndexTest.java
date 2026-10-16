package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 向量索引测试：用一个「关键词 → 维度」的假模型，让余弦相似度完全可预测，
 * 这样不需要真的加载 BGE 模型就能验证切分、去重、阈值与降级这几条逻辑。
 */
class EmbeddingPolicyIndexTest {

    private final PolicyDataMapper policyDataMapper = mock(PolicyDataMapper.class);

    @Test
    void semanticSearchReturnsThePolicyAboutTheSameTopicFirst() {
        stubMapper(List.of(
                policy(1L, "高校毕业生创业补贴", "补贴", "面向高校毕业生的一次性创业补贴"),
                policy(2L, "创业场地扶持", "场地", "提供办公场地租金减免")));

        EmbeddingPolicyIndex index = newIndex(0.0);
        index.afterPropertiesSet();

        List<PolicyData> hits = index.search("场地租金能减免吗", 5);

        assertEquals(2L, hits.get(0).getId(), "「场地」问句应召回场地扶持政策");
    }

    @Test
    void policySplitIntoManyChunksIsReturnedOnlyOnce() {
        stubMapper(List.of(policy(3L, "场地扶持", "场地", "场地".repeat(1200))));

        EmbeddingPolicyIndex index = newIndex(0.0);
        index.afterPropertiesSet();

        List<PolicyData> hits = index.search("场地", 5);

        assertEquals(1, hits.size(), "同一条政策的多个分片应合并成一条结果");
        assertEquals(3L, hits.get(0).getId());
    }

    @Test
    void minScoreFiltersOutUnrelatedPolicies() {
        stubMapper(List.of(
                policy(1L, "高校毕业生创业补贴", "补贴", "面向高校毕业生的一次性创业补贴"),
                policy(2L, "创业场地扶持", "场地", "提供办公场地租金减免")));

        EmbeddingPolicyIndex index = newIndex(0.99);
        index.afterPropertiesSet();

        List<PolicyData> hits = index.search("场地租金能减免吗", 5);

        assertEquals(1, hits.size(), "相似度低于阈值的政策应被过滤掉");
        assertEquals(2L, hits.get(0).getId());
    }

    @Test
    void rebuildFailureDegradesToEmptyResultWithoutThrowing() {
        when(policyDataMapper.selectList(any())).thenThrow(new IllegalStateException("db down"));

        EmbeddingPolicyIndex index = newIndex(0.0);

        assertDoesNotThrow(index::afterPropertiesSet, "索引构建失败不应让应用起不来");
        assertTrue(index.search("场地", 5).isEmpty(), "索引为空时应返回空结果，由上层退回关键词检索");
    }

    @Test
    void emptyPolicyTableLeavesIndexEmpty() {
        stubMapper(List.of());

        EmbeddingPolicyIndex index = newIndex(0.0);
        index.afterPropertiesSet();

        assertTrue(index.search("场地", 5).isEmpty());
    }

    private EmbeddingPolicyIndex newIndex(double minScore) {
        return new EmbeddingPolicyIndex(policyDataMapper, new TopicEmbeddingModel(),
                DocumentSplitters.recursive(500, 50), 10, minScore);
    }

    private void stubMapper(List<PolicyData> policies) {
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

    /**
     * 极简假模型：把「补贴 / 场地 / 税收」映射到三个维度。
     *
     * <p>与真实的 BGE 模型一样只覆盖 {@code embedAll} 这一个扩展点——
     * 接口里的 {@code embed(String)} / {@code embed(TextSegment)} 最终都会回退到它。</p>
     */
    private static final class TopicEmbeddingModel implements EmbeddingModel {

        private static final List<String> TOPICS = List.of("补贴", "场地", "税收");

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            return Response.from(textSegments.stream()
                    .map(segment -> Embedding.from(vectorOf(segment.text())))
                    .collect(Collectors.toList()));
        }

        private static float[] vectorOf(String text) {
            float[] vector = new float[TOPICS.size()];
            for (int i = 0; i < TOPICS.size(); i++) {
                vector[i] = text.contains(TOPICS.get(i)) ? 1f : 0f;
            }
            return vector;
        }
    }
}
