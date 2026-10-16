package com.mingde.creativeengine.aop;

import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.exception.BusinessException;
import com.mingde.creativeengine.security.UserContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

/**
 * {@link Idempotent} 的环绕通知：用「进行中」标记实现并发去重。
 *
 * <p>做法只有三步——进来时把请求键 {@code putIfAbsent} 进缓存，插不进去说明已经有一条同样的请求
 * 在处理中，直接拒绝；插进去了说明抢到了，正常执行；无论成功失败，退出前把标记删掉。</p>
 *
 * <p>几个刻意的选择：</p>
 * <ol>
 *   <li><b>只记「进行中」，不记「已完成」</b>。第一次请求返回后立刻释放，用户过一会儿再问一遍同样的内容是
 *       正常业务，不该被拦。这也顺势解释了为什么不能靠一条内容去重记录挡所有重复。</li>
 *   <li><b>{@code finally} 里必须释放</b>。否则模型调用抛异常后这个键会被永久占住，用户再也问不了同一个问题。</li>
 *   <li><b>缓存设了过期时间</b>，只是兜底：万一释放逻辑被绕过（比如 JVM 直接挂掉），标记也不会一直留在内存里。</li>
 *   <li><b>顺序排在业务日志切面外层</b>（见 {@code @Order}），被拦掉的重复请求不会留下一条空的会话记录。</li>
 * </ol>
 */
@Aspect
@Component
@Order(0)
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    /**
     * 进行中标记的兜底有效期。设得比模型调用超时更长，避免正常请求还没跑完标记就过期、把重复请求放进来。
     */
    private static final Duration IN_FLIGHT_TTL = Duration.ofMinutes(5);

    private final Cache<String, Boolean> inFlight = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(IN_FLIGHT_TTL)
            .build();

    @Around("@annotation(idempotent)")
    public Object guard(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint);

        // putIfAbsent 返回非 null 说明键已存在，即有一条同样的请求正在处理中
        if (inFlight.asMap().putIfAbsent(key, Boolean.TRUE) != null) {
            log.warn("拦截到重复提交：{}", key);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            inFlight.invalidate(key);
        }
    }

    /**
     * 键 = 用户 + 方法 + 入参内容指纹。
     *
     * <p>带上用户是必须的：不同用户在同一时刻问了同一个热门问题，不该互相影响。
     * 入参用 {@code Arrays.deepHashCode} 取内容指纹（Map、record 都实现了基于内容的 hashCode），
     * 这样「同一次点击被重发」和「用户换了问题」能区分开。</p>
     */
    private String buildKey(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return UserContext.getUserId() + "#" + method.toGenericString()
                + "#" + Arrays.deepHashCode(joinPoint.getArgs());
    }
}
