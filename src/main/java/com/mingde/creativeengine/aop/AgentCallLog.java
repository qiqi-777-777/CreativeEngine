package com.mingde.creativeengine.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「需要记录 Agent 调用日志」的接口方法。
 *
 * <p>改造前每个接口都要手写一段 {@code agentLogPublisher.publish(new AgentLogMessage(...))}，
 * 五个接口五段几乎一样的代码，字段顺序写错也不会有任何提示。抽成注解之后，
 * 方法体里只剩业务逻辑，日志统一交给 {@link AgentCallLogAspect}。</p>
 *
 * <p><b>为什么用 SpEL 而不是约定「第几个参数是输入」</b>：真实项目里的 {@code @Log}、{@code @Lock}
 * 都是这个做法。按位置约定既看不出来意图，参数顺序一变就会静默记错。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentCallLog {

    /** Agent 类型，写入 agent_session_log.agent_type。 */
    String agentType();

    /**
     * 从返回值里覆盖 agentType 的 key，用于路由场景：
     * 注解上写的是入口名（router），实际答话的是哪个子 Agent 要等返回值才知道。
     */
    String agentTypeKey() default "";

    /** 取「输入内容」的 SpEL 表达式，用 {@code #参数名} 引用方法入参。 */
    String inputSpel();

    /** 取「关联业务 ID」的 SpEL 表达式；没有关联业务时可以不写。 */
    String relatedSpel() default "";

    /** 从返回值 data 里取「输出内容」的 key。 */
    String answerKey() default "answer";
}
