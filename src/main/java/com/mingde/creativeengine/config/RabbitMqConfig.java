package com.mingde.creativeengine.config;

import com.mingde.creativeengine.mq.AgentLogPublisher;
import com.mingde.creativeengine.mq.DirectAgentLogPublisher;
import com.mingde.creativeengine.mq.RabbitAgentLogPublisher;
import com.mingde.creativeengine.service.agent.AgentLogService;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 日志的异步链路装配。
 *
 * <p>默认（{@code app.mq.enabled=false}）直接同步写库，本地零依赖即可运行；
 * 打开后改为投递到 RabbitMQ 由消费者异步落库，把写日志的耗时从用户问答链路里摘出去。</p>
 *
 * <p>交换机、队列与绑定在这里声明，由 RabbitAdmin 在首次连接时自动创建，
 * 省去人工登录管控台建队列的步骤。</p>
 */
@Configuration
public class RabbitMqConfig {

    /** 日志交换机。 */
    public static final String EXCHANGE = "creative-engine.agent-log.exchange";

    /** 日志队列。 */
    public static final String QUEUE = "creative-engine.agent-log";

    /** 路由键。 */
    public static final String ROUTING_KEY = "agent.log";

    @Bean
    public AgentLogPublisher agentLogPublisher(AgentLogService agentLogService,
                                               ObjectProvider<RabbitTemplate> rabbitTemplates,
                                               @Value("${app.mq.enabled:false}") boolean mqEnabled) {
        RabbitTemplate rabbitTemplate = rabbitTemplates.getIfAvailable();
        if (mqEnabled && rabbitTemplate != null) {
            return new RabbitAgentLogPublisher(rabbitTemplate, agentLogService);
        }
        return new DirectAgentLogPublisher(agentLogService);
    }

    /**
     * 消息体用 JSON 序列化：不依赖 JDK 序列化对类版本的强绑定，也方便在管控台直接看内容。
     */
    @Bean
    public Jackson2JsonMessageConverter agentLogMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
    public Queue agentLogQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
    public DirectExchange agentLogExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
    public Binding agentLogBinding(Queue agentLogQueue, DirectExchange agentLogExchange) {
        return BindingBuilder.bind(agentLogQueue).to(agentLogExchange).with(ROUTING_KEY);
    }
}
