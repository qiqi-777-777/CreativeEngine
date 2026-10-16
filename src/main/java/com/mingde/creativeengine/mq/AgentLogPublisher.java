package com.mingde.creativeengine.mq;

/**
 * Agent 调用日志的投递入口。
 *
 * <p>默认同步写库，开启 MQ 后改为投递到 RabbitMQ 由消费者异步落库，
 * 把写日志的耗时从用户问答的请求链路里摘出去。</p>
 */
public interface AgentLogPublisher {

    void publish(AgentLogMessage message);
}
