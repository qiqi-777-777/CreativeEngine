package com.mingde.creativeengine.config;

import com.mingde.creativeengine.service.agent.aiservice.AgentAiServiceFactory;
import com.mingde.creativeengine.service.agent.aiservice.BpAiService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;
import com.mingde.creativeengine.service.agent.aiservice.OccupationAiService;
import com.mingde.creativeengine.service.agent.aiservice.PolicyAiService;
import com.mingde.creativeengine.service.agent.aiservice.StartupAiService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 装配：统一提供大模型客户端与场景化 Agent。
 *
 * <p>DeepSeek 提供 OpenAI 兼容接口，因此使用 LangChain4j 的 OpenAiChatModel 接入，
 * 由框架统一负责请求构建、超时、重试与响应解析；上层 Agent 只关心业务数据与提示词。
 * 需要严格 JSON 输出的场景（职业推荐）使用独立的 json_object 模式模型实例。</p>
 */
@Configuration
public class LangChain4jConfig {

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.base-url:}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String modelName;

    @Value("${deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${deepseek.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${deepseek.timeout-seconds:60}")
    private long timeoutSeconds;

    @Value("${deepseek.max-retries:1}")
    private int maxRetries;

    @Value("${deepseek.log-requests:false}")
    private boolean logRequests;

    @Value("${deepseek.log-responses:false}")
    private boolean logResponses;

    /**
     * 通用对话模型：政策解读、职业规划、BP 写作、创业咨询都走这个实例。
     */
    @Primary
    @Bean
    public ChatModel deepSeekChatModel() {
        return buildChatModel(null);
    }

    /**
     * JSON 输出模型：DeepSeek 支持 json_object 模式但不支持严格的 JSON-Schema，
     * 因此需要结构化结果时单独使用这个实例，避免影响自然语言回复的 Agent。
     */
    @Bean
    public ChatModel deepSeekJsonChatModel() {
        return buildChatModel(ResponseFormat.JSON);
    }

    /**
     * 会话记忆存储：当前为进程内实现，后续可替换为 Redis 等实现而不影响 Agent 代码。
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public PolicyAiService policyAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel) {
        return AgentAiServiceFactory.policyAiService(chatModel);
    }

    @Bean
    public CareerAiService careerAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel) {
        return AgentAiServiceFactory.careerAiService(chatModel);
    }

    @Bean
    public BpAiService bpAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel) {
        return AgentAiServiceFactory.bpAiService(chatModel);
    }

    @Bean
    public OccupationAiService occupationAiService(@Qualifier("deepSeekJsonChatModel") ChatModel chatModel) {
        return AgentAiServiceFactory.occupationAiService(chatModel);
    }

    @Bean
    public StartupAiService startupAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel,
                                             ChatMemoryStore chatMemoryStore) {
        return AgentAiServiceFactory.startupAiService(chatModel, chatMemoryStore);
    }

    private ChatModel buildChatModel(ResponseFormat responseFormat) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(resolveBaseUrl())
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses);
        if (responseFormat != null) {
            builder.responseFormat(responseFormat);
        }
        return builder.build();
    }

    /**
     * LangChain4j 需要的是服务根地址（如 https://api.deepseek.com/v1），
     * 这里兼容旧配置中带 /chat/completions 的完整地址。
     */
    private String resolveBaseUrl() {
        return resolveBaseUrl(baseUrl, apiUrl);
    }

    static String resolveBaseUrl(String configuredBaseUrl, String apiUrl) {
        if (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty()) {
            return trimTrailingSlash(configuredBaseUrl.trim());
        }
        String url = apiUrl == null ? "" : apiUrl.trim();
        int index = url.indexOf("/chat/completions");
        if (index > 0) {
            return url.substring(0, index);
        }
        return trimTrailingSlash(url);
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
