package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;

import java.util.List;

/**
 * 政策检索入口。
 *
 * <p>默认实现只走关键词（{@link KeywordPolicyRetriever}），开启 {@code app.rag.enabled}
 * 后换成「语义 + 关键词」混合召回（{@link HybridPolicyRetriever}）。
 * Agent 工具只依赖这个接口，不感知底层是 LIKE 还是向量检索。</p>
 */
public interface PolicyRetriever {

    /**
     * 按相关度返回最相关的若干条政策。
     *
     * @param query      用户的自然语言问题或关键词
     * @param maxResults 最多返回条数
     */
    List<PolicyData> retrieve(String query, int maxResults);
}
