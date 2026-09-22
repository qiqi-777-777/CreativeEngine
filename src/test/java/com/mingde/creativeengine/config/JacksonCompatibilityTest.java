package com.mingde.creativeengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 兼容性检查：LangChain4j 1.20 需要 Jackson 2.22，而 Spring Boot 3.1.5 默认管理 2.15，
 * 因此 pom 中显式覆盖了 jackson-bom 版本。这里用 Spring 自己的 Jackson 构建器做一次
 * 运行时校验，确保升级后的 Jackson 与当前 Spring 版本能正常协作。
 */
class JacksonCompatibilityTest {

    @Test
    void springJacksonBuilderStillWorksWithUpgradedJackson() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

        String json = mapper.writeValueAsString(Map.of("name", "测试", "count", 2));
        assertTrue(json.contains("测试"), json);

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue("{\"count\":2}", Map.class);
        assertEquals(2, parsed.get("count"));
    }
}
