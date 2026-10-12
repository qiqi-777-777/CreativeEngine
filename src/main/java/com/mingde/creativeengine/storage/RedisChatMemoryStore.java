package com.mingde.creativeengine.storage;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Redis 版会话记忆存储，接入 LangChain4j 的 {@link ChatMemoryStore}。
 *
 * <p>改造前用的是 {@code InMemoryChatMemoryStore}：会话只存在单个进程内，
 * 用户在小程序里问到一半遇到发版/多实例路由，上下文就丢了。</p>
 *
 * <p>换成 Redis 后：多实例共享同一份会话、进程重启不丢上下文、并且按最后活跃时间
 * 自动过期回收，避免会话数据无限堆积。消息体沿用 LangChain4j 官方提供的
 * JSON 编解码器，保证反序列化后能还原成正确的消息类型（含工具调用消息）。</p>
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);

    /** key 前缀，与 LangChain4j 的 memoryId 拼成唯一键。 */
    static final String KEY_PREFIX = "agent:memory:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(key(memoryId));
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (RuntimeException e) {
            // 会话数据损坏时清理掉，避免每次问答都反序列化失败
            log.warn("会话记忆反序列化失败，已清理 memoryId={}", memoryId, e);
            deleteMessages(memoryId);
            return List.of();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            deleteMessages(memoryId);
            return;
        }
        // 每次写入都刷新 TTL：以"最后一次活跃"为准计算过期
        redisTemplate.opsForValue().set(key(memoryId), ChatMessageSerializer.messagesToJson(messages), ttl);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
