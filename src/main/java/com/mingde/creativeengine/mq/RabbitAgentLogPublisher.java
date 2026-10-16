package com.mingde.creativeengine.mq;

import com.mingde.creativeengine.config.RabbitMqConfig;
import com.mingde.creativeengine.service.agent.AgentLogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 开启 MQ 时的实现：把日志作为消息投给 RabbitMQ，由消费者异步落库。
 *
 * <p>投递失败（broker 抖动、队列未声明等）时降级为同步写库：
 * 日志是旁路数据，绝不能因为 MQ 不可用就让用户的问答请求失败。</p>
 */
public class RabbitAgentLogPublisher implements AgentLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitAgentLogPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AgentLogService agentLogService;

    public RabbitAgentLogPublisher(RabbitTemplate rabbitTemplate, AgentLogService agentLogService) {
        this.rabbitTemplate = rabbitTemplate;
        this.agentLogService = agentLogService;
    }

    @Override
    public void publish(AgentLogMessage message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, message);
        } catch (AmqpException e) {
            log.warn("Agent 日志投递 MQ 失败，降级为同步写库：{}", e.getMessage());
            agentLogService.saveLog(message.userId(), message.agentType(),
                    message.inputText(), message.outputText(), message.relatedId());
        }
    }
}
