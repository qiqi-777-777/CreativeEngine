package com.mingde.creativeengine.config;

import com.mingde.creativeengine.service.agent.aiservice.BpAiService;
import com.mingde.creativeengine.service.agent.aiservice.CareerAiService;
import com.mingde.creativeengine.service.agent.aiservice.OccupationAiService;
import com.mingde.creativeengine.service.agent.aiservice.PolicyAiService;
import com.mingde.creativeengine.service.agent.aiservice.StartupAiService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

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
        context.register(LangChain4jConfig.class);

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
        } finally {
            context.close();
        }
    }
}
