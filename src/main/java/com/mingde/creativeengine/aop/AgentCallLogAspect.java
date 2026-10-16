package com.mingde.creativeengine.aop;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.mq.AgentLogMessage;
import com.mingde.creativeengine.mq.AgentLogPublisher;
import com.mingde.creativeengine.security.UserContext;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link AgentCallLog} 的环绕通知：方法正常返回后，按注解里的 SpEL 从入参与返回值里
 * 提取字段并投递给 {@link AgentLogPublisher}。
 *
 * <p>三条原则：</p>
 * <ol>
 *   <li><b>只记成功</b>。方法抛异常时不写日志——异常路径下没有答案，硬记一条空答案反而会污染会话记录；</li>
 *   <li><b>解析失败不抛</b>。SpEL 写错、参数名对不上、返回结构变了，都只记 warn 并把该字段留空，
 *       绝不能因为写日志把用户的问答请求带崩；</li>
 *   <li><b>只拦带注解的方法</b>。切点用 {@code @annotation}，不给所有 Controller 都套一层代理。</li>
 * </ol>
 */
@Aspect
@Component
public class AgentCallLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AgentCallLogAspect.class);

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final AgentLogPublisher agentLogPublisher;

    public AgentCallLogAspect(AgentLogPublisher agentLogPublisher) {
        this.agentLogPublisher = agentLogPublisher;
    }

    @Around("@annotation(agentCallLog)")
    public Object record(ProceedingJoinPoint joinPoint, AgentCallLog agentCallLog) throws Throwable {
        Object result = joinPoint.proceed();

        // 兜底再包一层：投递本身也可能出错，但这里绝不能让异常冒到业务方法外面
        try {
            publish(joinPoint, agentCallLog, result);
        } catch (Exception e) {
            log.warn("Agent 调用日志记录失败，已跳过：{}", e.getMessage());
        }
        return result;
    }

    private void publish(ProceedingJoinPoint joinPoint, AgentCallLog agentCallLog, Object result) {
        Map<?, ?> data = extractData(result);

        String agentType = agentCallLog.agentType();
        if (!agentCallLog.agentTypeKey().isBlank()) {
            agentType = text(data.get(agentCallLog.agentTypeKey()), agentType);
        }
        String input = text(evaluate(joinPoint, agentCallLog.inputSpel()), "");
        String answer = text(data.get(agentCallLog.answerKey()), "");
        String related = text(evaluate(joinPoint, agentCallLog.relatedSpel()), "");

        agentLogPublisher.publish(new AgentLogMessage(
                UserContext.getUserId(), agentType, input, answer, related));
    }

    /** 约定接口返回 {@code Result<Map<String, Object>>}，日志字段从 data 里取。 */
    private Map<?, ?> extractData(Object result) {
        if (result instanceof Result<?> wrapper && wrapper.getData() instanceof Map<?, ?> data) {
            return data;
        }
        return Map.of();
    }

    /**
     * 执行注解上的 SpEL。参数名依赖 {@code -parameters} 编译开关（项目已在 maven-compiler-plugin 开启），
     * 万一取不到参数名，还留了 {@code #p0 / #p1} 这种按位置引用作为退路。
     */
    private Object evaluate(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            Object[] args = joinPoint.getArgs();
            String[] names = PARAMETER_NAMES.getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());

            StandardEvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < args.length; i++) {
                if (names != null && i < names.length && names[i] != null) {
                    context.setVariable(names[i], args[i]);
                }
                context.setVariable("p" + i, args[i]);
            }
            return SPEL_PARSER.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            log.warn("Agent 日志的 SpEL 表达式求值失败：{}（{}）", expression, e.getMessage());
            return null;
        }
    }

    private static String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
}
