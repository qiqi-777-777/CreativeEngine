package com.mingde.creativeengine.config;

import com.mingde.creativeengine.entity.Occupation;
import com.mingde.creativeengine.common.PageResult;
import com.mingde.creativeengine.entity.PolicyData;
import com.mingde.creativeengine.service.OccupationService;
import com.mingde.creativeengine.service.PolicyDataService;
import com.mingde.creativeengine.service.agent.aiservice.BpAiService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;
import com.mingde.creativeengine.service.agent.aiservice.OccupationAiService;
import com.mingde.creativeengine.service.agent.aiservice.PolicyAiService;
import com.mingde.creativeengine.service.agent.aiservice.StartupAiService;
import com.mingde.creativeengine.service.agent.tool.OccupationTools;
import com.mingde.creativeengine.service.agent.tool.PolicyTools;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Spring 装配测试：只加载 LangChain4j 配置，不依赖数据库和网络。
 *
 * <p>验证两个 ChatModel 实例（通用 / JSON）都被正确创建、AiService 代理可以正常装配，
 * 避免因为 Bean 类型冲突或限定符写错而只能在启动时才暴露问题。</p>
 */
class LangChain4jContextWiringTest {

    @Test
    void wiresChatModelsAndAgentProxies() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "deepseek.api-key", "test-key",
                "deepseek.base-url", "https://api.deepseek.com/v1",
                "deepseek.model", "deepseek-chat"
        )));

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        // 工具 Bean 是 AiService 的依赖，这里连同它们的数据服务一起装配
        context.register(LangChain4jConfig.class, ResilienceConfig.class, RagConfig.class,
                PolicyTools.class, OccupationTools.class);
        context.registerBean(PolicyDataService.class, LangChain4jContextWiringTest::stubPolicyDataService);
        context.registerBean(OccupationService.class, LangChain4jContextWiringTest::stubOccupationService);
        // 指标组件依赖 MeterRegistry，生产环境由 actuator 提供，这里注册一个内存实现
        context.registerBean(MeterRegistry.class, SimpleMeterRegistry::new);
        // 熔断/限流实例生产环境由 resilience4j-spring-boot3 依据 application.yml 装配，这里用默认配置替代
        context.registerBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults);
        context.registerBean(RateLimiterRegistry.class, RateLimiterRegistry::ofDefaults);

        try {
            context.refresh();

            assertEquals(2, context.getBeansOfType(ChatModel.class).size(),
                    "应存在通用模型与 JSON 模型两个实例");
            assertNotNull(context.getBean("deepSeekChatModel"));
            assertNotNull(context.getBean("deepSeekJsonChatModel"));

            assertNotNull(context.getBean(PolicyAiService.class));
            assertNotNull(context.getBean(CareerAiService.class));
            assertNotNull(context.getBean(BpAiService.class));
            assertNotNull(context.getBean(OccupationAiService.class));
            assertNotNull(context.getBean(StartupAiService.class));
            assertNotNull(context.getBean(ChatMemoryStore.class));

            assertNotNull(context.getBean(PolicyTools.class), "政策工具应注册为 Bean");
            assertNotNull(context.getBean(OccupationTools.class), "职业工具应注册为 Bean");
        } finally {
            context.close();
        }
    }

    private static PolicyDataService stubPolicyDataService() {
        return new PolicyDataService() {
            @Override
            public List<PolicyData> getAllPolicies() {
                return Collections.emptyList();
            }

            @Override
            public PolicyData getPolicyById(Long id) {
                return null;
            }

            @Override
            public PolicyData getPolicyByName(String policyName) {
                return null;
            }

            @Override
            public List<PolicyData> searchPolicies(String keyword) {
                return Collections.emptyList();
            }

            @Override
            public PageResult<PolicyData> pagePolicies(String keyword, int page, int size) {
                return PageResult.of(Collections.emptyList(), 0, page, size);
            }
        };
    }

    private static OccupationService stubOccupationService() {
        return new OccupationService() {
            @Override
            public List<String> getAllMajors() {
                return Collections.emptyList();
            }

            @Override
            public List<String> getAllSkills() {
                return Collections.emptyList();
            }

            @Override
            public List<String> getAllPersonalities() {
                return Collections.emptyList();
            }

            @Override
            public List<Occupation> matchOccupations(List<String> majors, List<String> skills, List<String> personalities) {
                return Collections.emptyList();
            }

            @Override
            public List<Occupation> matchFromDatabase(List<String> majors, List<String> skills, List<String> personalities) {
                return Collections.emptyList();
            }
        };
    }
}
