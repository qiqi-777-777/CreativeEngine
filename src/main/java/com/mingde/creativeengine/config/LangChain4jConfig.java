package com.mingde.creativeengine.config;

import com.mingde.creativeengine.observability.AgentTelemetry;
import com.mingde.creativeengine.resilience.ResilientChatModel;
import com.mingde.creativeengine.service.agent.aiservice.AgentAiServiceFactory;
import com.mingde.creativeengine.service.agent.aiservice.BpAiService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;
import com.mingde.creativeengine.service.agent.aiservice.OccupationAiService;
import com.mingde.creativeengine.service.agent.aiservice.PolicyAiService;
import com.mingde.creativeengine.service.agent.aiservice.StartupAiService;
import com.mingde.creativeengine.service.agent.tool.OccupationTools;
import com.mingde.creativeengine.service.agent.tool.PolicyTools;
import com.mingde.creativeengine.storage.RedisChatMemoryStore;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    public ChatModel deepSeekChatModel(AgentTelemetry agentTelemetry,
                                       CircuitBreaker modelCircuitBreaker,
                                       RateLimiter modelRateLimiter) {
        return buildChatModel(null, agentTelemetry, modelCircuitBreaker, modelRateLimiter);
    }

    /**
     * JSON 输出模型：DeepSeek 支持 json_object 模式但不支持严格的 JSON-Schema，
     * 因此需要结构化结果时单独使用这个实例，避免影响自然语言回复的 Agent。
     */
    @Bean
    public ChatModel deepSeekJsonChatModel(AgentTelemetry agentTelemetry,
                                           CircuitBreaker modelCircuitBreaker,
                                           RateLimiter modelRateLimiter) {
        return buildChatModel(ResponseFormat.JSON, agentTelemetry, modelCircuitBreaker, modelRateLimiter);
    }

    /**
     * 可观测组件：模型调用与工具调用的指标、带 traceId 的调用日志。
     */
    @Bean
    public AgentTelemetry agentTelemetry(MeterRegistry meterRegistry) {
        return new AgentTelemetry(meterRegistry);
    }

    /**
     * 会话记忆存储：开启 Redis 时用 {@code RedisChatMemoryStore} 让多实例共享会话、重启不丢，
     * 否则退回进程内实现，保证本地零依赖即可运行。
     */
    @Bean
    public ChatMemoryStore chatMemoryStore(ObjectProvider<StringRedisTemplate> redisTemplates,
                                           @Value("${app.redis.enabled:false}") boolean redisEnabled,
                                           @Value("${app.redis.chat-memory-ttl:30m}") String chatMemoryTtl) {
        StringRedisTemplate redisTemplate = redisTemplates.getIfAvailable();
        if (redisEnabled && redisTemplate != null) {
            return new RedisChatMemoryStore(redisTemplate, RedisConfig.parseTtl(chatMemoryTtl, Duration.ofMinutes(30)));
        }
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public PolicyAiService policyAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel,
                                           PolicyTools policyTools,
                                           AgentTelemetry agentTelemetry) {
        return AgentAiServiceFactory.policyAiService(chatModel, policyTools, agentTelemetry);
    }

    @Bean
    public CareerAiService careerAiService(@Qualifier("deepSeekChatModel") ChatModel chatModel,
                                           OccupationTools occupationTools,
                                           AgentTelemetry agentTelemetry) {
        return AgentAiServiceFactory.careerAiService(chatModel, occupationTools, agentTelemetry);
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

    private ChatModel buildChatModel(ResponseFormat responseFormat, AgentTelemetry agentTelemetry,
                                     CircuitBreaker circuitBreaker, RateLimiter rateLimiter) {
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
        // 挂上模型调用监听器：耗时、token、异常统一进指标与日志
        if (agentTelemetry != null) {
            builder.listeners(agentTelemetry.chatModelListener());
        }
        // 最外层再套限流与熔断：DeepSeek 抖动或配额被打满时，保护本服务的线程与响应时间
        return new ResilientChatModel(builder.build(), circuitBreaker, rateLimiter);
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
