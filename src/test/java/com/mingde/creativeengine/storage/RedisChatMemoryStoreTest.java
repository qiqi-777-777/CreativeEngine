package com.mingde.creativeengine.storage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis 版会话记忆存储测试：用一个 Map 模拟 Redis，验证消息编解码与 TTL 刷新逻辑。
 */
class RedisChatMemoryStoreTest {

    private static final Duration TTL = Duration.ofMinutes(30);

    @SuppressWarnings("unchecked")
    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final Map<String, String> backing = new HashMap<>();
    private final RedisChatMemoryStore store = new RedisChatMemoryStore(template, TTL);

    @BeforeEach
    void setUp() {
        when(template.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(call -> backing.get(call.getArgument(0)));
        doAnswer(call -> {
            backing.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(template.delete(anyString())).thenAnswer(call -> backing.remove(call.getArgument(0)) != null);
    }

    @Test
    void updateThenGetRoundTripsMessages() {
        List<ChatMessage> messages = List.of(UserMessage.from("你好"), AiMessage.from("你好呀"));

        store.updateMessages("session-1", messages);

        assertTrue(backing.containsKey("agent:memory:session-1"));
        List<ChatMessage> loaded = store.getMessages("session-1");
        assertEquals(2, loaded.size());
        assertEquals("你好", ((UserMessage) loaded.get(0)).singleText());
        assertEquals("你好呀", ((AiMessage) loaded.get(1)).text());
    }

    @Test
    void getMessagesReturnsEmptyWhenAbsent() {
        assertTrue(store.getMessages("missing").isEmpty());
    }

    @Test
    void emptyListClearsSession() {
        store.updateMessages("session-2", List.of(UserMessage.from("hi")));

        store.updateMessages("session-2", List.of());

        assertTrue(store.getMessages("session-2").isEmpty());
    }

    @Test
    void corruptedValueIsCleanedUp() {
        backing.put("agent:memory:broken", "{not-valid-json}");

        assertTrue(store.getMessages("broken").isEmpty());
        assertTrue(!backing.containsKey("agent:memory:broken"), "损坏的会话数据应被清理");
    }

    @Test
    void deleteRemovesSession() {
        store.updateMessages("session-3", List.of(UserMessage.from("hi")));

        store.deleteMessages("session-3");

        assertTrue(store.getMessages("session-3").isEmpty());
    }
}
