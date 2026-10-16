package com.mingde.creativeengine.config;

import com.mingde.creativeengine.mapper.PolicyDataMapper;
import com.mingde.creativeengine.rag.EmbeddingPolicyIndex;
import com.mingde.creativeengine.rag.HybridPolicyRetriever;
import com.mingde.creativeengine.rag.KeywordPolicyRetriever;
import com.mingde.creativeengine.rag.PolicyRetriever;
import com.mingde.creativeengine.rag.SemanticPolicySearch;
import com.mingde.creativeengine.service.PolicyDataService;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 政策检索（RAG）装配。
 *
 * <p>{@code app.rag.enabled=false}（默认）时只提供关键词检索，本地零依赖、不加载模型；
 * 打开后加载本地中文 Embedding 模型并构建向量索引，检索升级为「语义 + 关键词」混合召回。</p>
 */
@Configuration
public class RagConfig {

    /**
     * 本地中文 Embedding 模型（BGE small zh，ONNX 离线推理）。
     *
     * <p>选本地模型而不是调云端 embedding 接口的原因：政策正文不必出网，
     * 也没有按量计费，模型文件随 jar 一起打包，不需要额外下载。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
    public EmbeddingModel policyEmbeddingModel() {
        return new BgeSmallZhV15EmbeddingModel();
    }

    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
    public SemanticPolicySearch policySemanticSearch(PolicyDataMapper policyDataMapper,
                                                     EmbeddingModel policyEmbeddingModel,
                                                     @Value("${app.rag.chunk-size:500}") int chunkSize,
                                                     @Value("${app.rag.chunk-overlap:50}") int chunkOverlap,
                                                     @Value("${app.rag.candidate-size:10}") int candidateSize,
                                                     @Value("${app.rag.min-score:0.6}") double minScore) {
        return new EmbeddingPolicyIndex(policyDataMapper, policyEmbeddingModel,
                DocumentSplitters.recursive(chunkSize, chunkOverlap), candidateSize, minScore);
    }

    /**
     * 检索入口：有语义检索能力时用混合召回，否则退回纯关键词。
     *
     * <p>用 {@link ObjectProvider} 而不是直接注入，是为了让「开不开 RAG」只由配置决定，
     * 不必把上层调用方拆成两套装配。</p>
     */
    @Bean
    public PolicyRetriever policyRetriever(PolicyDataService policyDataService,
                                           ObjectProvider<SemanticPolicySearch> semanticSearches,
                                           @Value("${app.rag.candidate-size:10}") int candidateSize) {
        SemanticPolicySearch semanticSearch = semanticSearches.getIfAvailable();
        if (semanticSearch != null) {
            return new HybridPolicyRetriever(policyDataService, semanticSearch, candidateSize);
        }
        return new KeywordPolicyRetriever(policyDataService);
    }
}
