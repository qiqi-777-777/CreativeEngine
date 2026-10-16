package com.mingde.creativeengine.mq;

import com.mingde.creativeengine.service.agent.AgentLogService;

/**
 * 未开启 MQ 时的实现：直接同步写库。
 *
 * <p>保持改造前的行为，本地开发不装 RabbitMQ 也能完整跑通。</p>
 */
public class DirectAgentLogPublisher implements AgentLogPublisher {

    private final AgentLogService agentLogService;

    public DirectAgentLogPublisher(AgentLogService agentLogService) {
        this.agentLogService = agentLogService;
    }

    @Override
    public void publish(AgentLogMessage message) {
        agentLogService.saveLog(message.userId(), message.agentType(),
                message.inputText(), message.outputText(), message.relatedId());
    }
}
