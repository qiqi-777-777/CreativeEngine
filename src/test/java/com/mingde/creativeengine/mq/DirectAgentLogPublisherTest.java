package com.mingde.creativeengine.mq;

import com.mingde.creativeengine.service.agent.AgentLogService;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 未开启 MQ 时的投递实现：应当保持改造前的同步写库行为。
 */
class DirectAgentLogPublisherTest {

    private final AgentLogService agentLogService = mock(AgentLogService.class);
    private final DirectAgentLogPublisher publisher = new DirectAgentLogPublisher(agentLogService);

    @Test
    void writesLogSynchronously() {
        publisher.publish(new AgentLogMessage(7L, "policy", "问题", "答案", "12"));

        verify(agentLogService).saveLog(7L, "policy", "问题", "答案", "12");
    }
}
