package com.mingde.creativeengine.mq;

/**
 * Agent 调用日志的异步消息体。
 *
 * <p>字段与 {@code AgentLogService.saveLog} 一一对应，消费端拿到就能直接落库，
 * 不需要再解析原始请求。</p>
 */
public record AgentLogMessage(
        Long userId,
        String agentType,
        String inputText,
        String outputText,
        String relatedId) {
}
