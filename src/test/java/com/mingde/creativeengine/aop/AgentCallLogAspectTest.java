package com.mingde.creativeengine.aop;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.mq.AgentLogMessage;
import com.mingde.creativeengine.mq.AgentLogPublisher;
import com.mingde.creativeengine.security.UserContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Agent 调用日志切面测试。
 *
 * <p>用真实的 Spring AOP 代理跑（而不是手工模拟 JoinPoint），这样 SpEL 取参、
 * 代理是否生效、异常传播这些行为都与线上一致。</p>
 */
class AgentCallLogAspectTest {

    private final AgentLogPublisher publisher = mock(AgentLogPublisher.class);

    private AnnotationConfigApplicationContext context;
    private ProbeAgent probe;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.registerBean(AgentLogPublisher.class, () -> publisher);
        context.registerBean(AgentCallLogAspect.class, () -> new AgentCallLogAspect(publisher));
        context.registerBean(ProbeAgent.class, ProbeAgent::new);
        context.register(AopConfig.class);
        context.refresh();

        probe = context.getBean(ProbeAgent.class);
        UserContext.set(7L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        context.close();
    }

    @Test
    void recordsFieldsFromArgumentsAndReturnValue() {
        probe.ask("应届生能领什么补贴", 12L);

        AgentLogMessage message = captured();
        assertEquals(7L, message.userId(), "userId 取自 UserContext，方法入参里并没有它");
        assertEquals("policy", message.agentType());
        assertEquals("应届生能领什么补贴", message.inputText());
        assertEquals("答案：应届生能领什么补贴", message.outputText());
        assertEquals("12", message.relatedId());
    }

    @Test
    void agentTypeFromResultOverridesAnnotation() {
        probe.route("问题", "career");
        probe.route("问题", "");

        ArgumentCaptor<AgentLogMessage> captor = ArgumentCaptor.forClass(AgentLogMessage.class);
        verify(publisher, times(2)).publish(captor.capture());

        assertEquals("career", captor.getAllValues().get(0).agentType(), "有路由结果时应记录真正答话的子 Agent");
        assertEquals("router", captor.getAllValues().get(1).agentType(), "路由为空时退回注解上的入口名");
    }

    @Test
    void relatedIdAndAnswerKeyComeFromRequestMapAndResult() {
        Map<String, Object> request = new HashMap<>();
        request.put("projectName", "校园二手书平台");

        probe.generateBp(request);

        AgentLogMessage message = captured();
        assertEquals("校园二手书平台", message.relatedId());
        assertEquals("BP 正文", message.outputText(), "answerKey 指向 content 时应取到 BP 正文");
    }

    @Test
    void brokenSpelStillPublishesAndDoesNotBreakTheCall() {
        Result<Map<String, Object>> result = probe.brokenSpel("问题");

        assertEquals(0, result.getCode(), "SpEL 写错不应影响业务返回值");
        assertEquals("", captured().inputText(), "取不到的字段留空，而不是把异常抛给调用方");
    }

    @Test
    void exceptionSkipsLoggingAndStillPropagates() {
        assertThrows(IllegalStateException.class, () -> probe.failing("问题"));

        verify(publisher, never()).publish(any());
    }

    @Test
    void unannotatedMethodIsNotIntercepted() {
        assertEquals("no-log", probe.notAnnotated());

        verify(publisher, never()).publish(any());
    }

    private AgentLogMessage captured() {
        ArgumentCaptor<AgentLogMessage> captor = ArgumentCaptor.forClass(AgentLogMessage.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AopConfig {
    }

    /**
     * 探针 Bean：只用来触发切面，方法体不关心业务。
     */
    public static class ProbeAgent {

        @AgentCallLog(agentType = "policy", inputSpel = "#question", relatedSpel = "#policyId")
        public Result<Map<String, Object>> ask(String question, Long policyId) {
            return payload("answer", "答案：" + question);
        }

        @AgentCallLog(agentType = "router", agentTypeKey = "route", inputSpel = "#question")
        public Result<Map<String, Object>> route(String question, String route) {
            Map<String, Object> data = new HashMap<>();
            data.put("answer", "路由答案");
            data.put("route", route);
            return Result.success(data);
        }

        @AgentCallLog(agentType = "bp", inputSpel = "#request.toString()",
                relatedSpel = "#request.get('projectName')", answerKey = "content")
        public Result<Map<String, Object>> generateBp(Map<String, Object> request) {
            return payload("content", "BP 正文");
        }

        @AgentCallLog(agentType = "policy", inputSpel = "#missingVariable")
        public Result<Map<String, Object>> brokenSpel(String question) {
            return payload("answer", "照常返回");
        }

        @AgentCallLog(agentType = "policy", inputSpel = "#question")
        public Result<Map<String, Object>> failing(String question) {
            throw new IllegalStateException("模型挂了");
        }

        public String notAnnotated() {
            return "no-log";
        }

        private static Result<Map<String, Object>> payload(String key, String value) {
            Map<String, Object> data = new HashMap<>();
            data.put(key, value);
            return Result.success(data);
        }
    }
}
