package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.PolicyDataService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 混合召回：语义检索 + 关键词检索，再用 RRF 融合两路结果。
 *
 * <p><b>为什么两路都要</b>：语义能把「应届生能领什么钱」召回到写着「高校毕业生一次性创业补贴」
 * 的政策上，但对政策名、文号这类精确字符串反而不如 LIKE；关键词能保证精确命中，
 * 但换个说法就问不到了。两者互补。</p>
 *
 * <p><b>融合为什么用 RRF（Reciprocal Rank Fusion）</b>：它只看两路各自的排名，不看相似度
 * 与关键词命中数的具体取值，因此不存在「相似度 0.8 和命中 2 个词怎么加权」这种需要反复
 * 调的参数。公式为 {@code score(d) = Σ 1 / (k + rank(d))}，k 取 60（原论文经验值），
 * 作用是压低「单路第一名」的绝对优势，让两路都命中的文档浮上来。</p>
 */
public class HybridPolicyRetriever implements PolicyRetriever {

    /** RRF 平滑常数，来自原论文的经验值。 */
    private static final int RRF_K = 60;

    private final PolicyDataService policyDataService;
    private final SemanticPolicySearch semanticSearch;

    /** 语义召回的候选分片数，取比最终条数更大，避免一条长政策占满全部名额。 */
    private final int candidateSize;

    public HybridPolicyRetriever(PolicyDataService policyDataService,
                                 SemanticPolicySearch semanticSearch,
                                 int candidateSize) {
        this.policyDataService = policyDataService;
        this.semanticSearch = semanticSearch;
        this.candidateSize = candidateSize;
    }

    @Override
    public List<PolicyData> retrieve(String query, int maxResults) {
        String trimmed = query == null ? "" : query.trim();

        List<PolicyData> keywordHits = cap(policyDataService.searchPolicies(trimmed), candidateSize);
        if (trimmed.isEmpty()) {
            // 空查询没有语义可言，直接返回政策列表的前几条
            return cap(keywordHits, maxResults);
        }

        List<PolicyData> semanticHits = semanticSearch.search(trimmed, candidateSize);
        if (semanticHits.isEmpty()) {
            // 索引没建好或没有任何分片过阈值：退回纯关键词，保持改造前后的行为
            return cap(keywordHits, maxResults);
        }

        return fuse(semanticHits, keywordHits, maxResults);
    }

    /**
     * RRF 融合：按 {@code 1 / (k + rank)} 累加两路的贡献，再按总分降序取前 N 条。
     *
     * <p>用 {@link LinkedHashMap} 是为了分数相同时保持稳定顺序（语义结果在前），
     * 否则 HashMap 的遍历顺序会让同样输入产生不同排序。</p>
     */
    static List<PolicyData> fuse(List<PolicyData> semanticHits, List<PolicyData> keywordHits, int maxResults) {
        Map<Long, PolicyData> byId = new LinkedHashMap<>();
        Map<Long, Double> scores = new LinkedHashMap<>();

        accumulate(semanticHits, byId, scores);
        accumulate(keywordHits, byId, scores);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> byId.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    private static void accumulate(List<PolicyData> hits, Map<Long, PolicyData> byId, Map<Long, Double> scores) {
        for (int rank = 0; rank < hits.size(); rank++) {
            PolicyData policy = hits.get(rank);
            if (policy == null || policy.getId() == null) {
                continue;
            }
            byId.putIfAbsent(policy.getId(), policy);
            scores.merge(policy.getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
    }

    private static List<PolicyData> cap(List<PolicyData> policies, int maxResults) {
        if (policies == null) {
            return List.of();
        }
        return policies.size() <= maxResults ? policies : policies.subList(0, maxResults);
    }
}
