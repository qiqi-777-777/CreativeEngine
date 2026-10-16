package com.mingde.creativeengine.mq;

import com.mingde.creativeengine.config.RabbitMqConfig;
import com.mingde.creativeengine.service.agent.AgentLogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Agent 日志消费者：把队列里的日志消息落到数据库。
 *
 * <p>只在 {@code app.mq.enabled=true} 时注册为 Bean，因此关闭 MQ 时不会产生任何连接尝试。</p>
 *
 * <p>落库由 {@code AgentLogService.saveLog} 完成，它内部已经吞掉异常（日志表缺失、
 * 写库失败都不影响主流程），所以这里不会向上抛异常，也就不存在消息反复重投的问题。</p>
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class AgentLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentLogConsumer.class);

    private final AgentLogService agentLogService;

    public AgentLogConsumer(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onAgentLog(AgentLogMessage message) {
        log.debug("收到 Agent 日志消息：userId={}, agentType={}", message.userId(), message.agentType());
        agentLogService.saveLog(message.userId(), message.agentType(),
                message.inputText(), message.outputText(), message.relatedId());
    }
}
