package com.mingde.creativeengine.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.mapper.PolicyDataMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 进程内政策向量索引：把政策正文切成小片、逐片向量化后放进 {@link InMemoryEmbeddingStore}。
 *
 * <p>几个关键取舍：</p>
 * <ol>
 *   <li><b>切分</b>：单条政策正文可能有几千字，整条embedding会让语义被稀释，
 *       按 {@code DocumentSplitters.recursive} 切成若干带重叠的片段，检索时按片命中再回到政策；</li>
 *   <li><b>拼接标题与关键词</b>：只向量化正文的话，问「XX 补贴有哪些」匹配不到政策标题；</li>
 *   <li><b>索引整体替换</b>：{@code store} 是 volatile 引用，重建时直接换新对象，
 *       检索侧不需要加锁也不会读到半成品。</li>
 * </ol>
 *
 * <p>数据量在千条量级时进程内索引完全够用；换成 Milvus / PgVector 时只要替换 store，
 * 上层 {@link SemanticPolicySearch} 的用法不变。</p>
 */
public class EmbeddingPolicyIndex implements SemanticPolicySearch, InitializingBean {

    static final String META_POLICY_ID = "policyId";

    private static final Logger log = LoggerFactory.getLogger(EmbeddingPolicyIndex.class);

    private final PolicyDataMapper policyDataMapper;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;

    /** 送去向量库检索的分片数，取比最终条数大一些，避免一条长政策占满名额。 */
    private final int candidateSize;

    /** 余弦相似度下限，低于该值的分片视为不相关。 */
    private final double minScore;

    private volatile InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

    public EmbeddingPolicyIndex(PolicyDataMapper policyDataMapper,
                                EmbeddingModel embeddingModel,
                                DocumentSplitter documentSplitter,
                                int candidateSize,
                                double minScore) {
        this.policyDataMapper = policyDataMapper;
        this.embeddingModel = embeddingModel;
        this.documentSplitter = documentSplitter;
        this.candidateSize = candidateSize;
        this.minScore = minScore;
    }

    /** 应用启动时构建索引。构建失败只记日志，检索会自动退回关键词。 */
    @Override
    public void afterPropertiesSet() {
        rebuild();
    }

    /**
     * 全量重建索引。政策由外部脚本批量导入，导入完成后调用本方法即可让语义检索看到新数据。
     */
    public synchronized void rebuild() {
        try {
            List<PolicyData> policies = policyDataMapper.selectList(new QueryWrapper<>());
            List<TextSegment> segments = new ArrayList<>();
            for (PolicyData policy : policies) {
                if (policy == null || policy.getId() == null) {
                    continue;
                }
                segments.addAll(documentSplitter.split(toDocument(policy)));
            }

            InMemoryEmbeddingStore<TextSegment> rebuilt = new InMemoryEmbeddingStore<>();
            if (segments.isEmpty()) {
                log.warn("政策库为空，语义索引未构建，政策检索将只走关键词");
            } else {
                long start = System.currentTimeMillis();
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                rebuilt.addAll(embeddings, segments);
                log.info("政策语义索引就绪：政策 {} 条，分片 {} 个，耗时 {} ms",
                        policies.size(), segments.size(), System.currentTimeMillis() - start);
            }
            this.store = rebuilt;
        } catch (Exception e) {
            log.error("政策语义索引构建失败，政策检索将只走关键词", e);
        }
    }

    @Override
    public List<PolicyData> search(String query, int maxResults) {
        String trimmed = query == null ? "" : query.trim();
        InMemoryEmbeddingStore<TextSegment> current = store;
        if (trimmed.isEmpty() || maxResults <= 0 || current.isEmpty()) {
            return List.of();
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(trimmed).content();
            List<EmbeddingMatch<TextSegment>> matches = current.search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(candidateSize)
                    .minScore(minScore)
                    .build()).matches();

            Set<Long> policyIds = new LinkedHashSet<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                Long policyId = policyIdOf(match.embedded());
                if (policyId != null) {
                    // matches 已按相似度降序，同一条政策的多个分片只保留最先出现的那次
                    policyIds.add(policyId);
                }
                if (policyIds.size() >= maxResults) {
                    break;
                }
            }
            return loadPolicies(policyIds);
        } catch (Exception e) {
            log.warn("语义检索失败，本次退化为关键词检索：{}", e.getMessage());
            return List.of();
        }
    }

    private List<PolicyData> loadPolicies(Set<Long> policyIds) {
        if (policyIds.isEmpty()) {
            return List.of();
        }
        Map<Long, PolicyData> loaded = policyDataMapper.selectBatchIds(policyIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PolicyData::getId, policy -> policy, (first, second) -> first, HashMap::new));

        // 保持向量检索给出的相关度顺序
        return policyIds.stream().map(loaded::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Document toDocument(PolicyData policy) {
        String text = text(policy.getPolicyName())
                + "\n关键词：" + text(policy.getKeywords())
                + "\n" + text(policy.getContent());
        return Document.from(text, Metadata.from(Map.of(
                META_POLICY_ID, String.valueOf(policy.getId()))));
    }

    private Long policyIdOf(TextSegment segment) {
        String value = segment.metadata().getString(META_POLICY_ID);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
