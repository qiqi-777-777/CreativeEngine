package com.mingde.creativeengine.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * base-url 解析测试：LangChain4j 需要服务根地址，而旧配置里是完整的
 * /chat/completions 地址，两种写法都必须能正常工作。
 */
class LangChain4jConfigTest {

    @Test
    void derivesBaseUrlFromFullChatCompletionsUrl() {
        assertEquals("https://api.deepseek.com/v1",
                LangChain4jConfig.resolveBaseUrl(null, "https://api.deepseek.com/v1/chat/completions"));
    }

    @Test
    void prefersExplicitBaseUrlAndTrimsTrailingSlash() {
        assertEquals("https://api.deepseek.com/v1",
                LangChain4jConfig.resolveBaseUrl("https://api.deepseek.com/v1/",
                        "https://api.deepseek.com/v1/chat/completions"));
    }

    @Test
    void keepsUrlWhenThereIsNoChatCompletionsSuffix() {
        assertEquals("https://api.deepseek.com",
                LangChain4jConfig.resolveBaseUrl("", "https://api.deepseek.com"));
    }

    @Test
    void returnsEmptyUrlWhenNothingConfigured() {
        assertEquals("", LangChain4jConfig.resolveBaseUrl(null, null));
    }
}
