package com.mingde.creativeengine.rag;

import com.mingde.creativeengine.entity.PolicyData;

import java.util.List;

/**
 * 语义召回：把查询与政策正文都转成向量，按余弦相似度取最相关的政策。
 *
 * <p>抽出接口是为了把「向量索引怎么存」和「怎么用」分开：
 * 当前实现是 {@link EmbeddingPolicyIndex}（进程内 {@code InMemoryEmbeddingStore}），
 * 数据量上来后换成 Milvus / PgVector 只需要再写一个实现类。</p>
 */
public interface SemanticPolicySearch {

    /**
     * 返回与查询语义最接近的政策，按相关度降序。失败或索引为空时返回空列表，不抛异常。
     */
    List<PolicyData> search(String query, int maxResults);
}
