package com.mingde.creativeengine.mq;

import com.mingde.creativeengine.config.RabbitMqConfig;
import com.mingde.creativeengine.service.agent.AgentLogService;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 开启 MQ 时的投递实现：验证消息发到正确的交换机与路由键，以及 broker 不可用时降级为同步写库。
 */
class RabbitAgentLogPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final AgentLogService agentLogService = mock(AgentLogService.class);
    private final RabbitAgentLogPublisher publisher = new RabbitAgentLogPublisher(rabbitTemplate, agentLogService);

    private final AgentLogMessage message = new AgentLogMessage(7L, "policy", "问题", "答案", "12");

    @Test
    void sendsMessageToExchangeInsteadOfWritingDirectly() {
        publisher.publish(message);

        verify(rabbitTemplate).convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, message);
        verify(agentLogService, never()).saveLog(any(), any(), any(), any(), any());
    }

    @Test
    void fallsBackToSyncWriteWhenBrokerUnavailable() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        publisher.publish(message);

        verify(agentLogService).saveLog(7L, "policy", "问题", "答案", "12");
    }
}
