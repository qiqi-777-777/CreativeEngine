package com.mingde.creativeengine.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「同一个用户、同一个请求，同一时刻只允许一个在处理中」的接口方法。
 *
 * <p><b>为什么需要</b>：这几个接口背后都是一次真实的模型调用，耗时以秒计、按 token 计费。
 * 小程序里点一下按钮如果没立刻出结果，用户会本能地再点几下，于是同一条问题并发打到后端，
 * 既浪费额度，也会让会话历史里出现多条一模一样的问题。</p>
 *
 * <p><b>它挡什么、不挡什么</b>（写清楚边界，免得后面误以为它是万能的）：</p>
 * <ul>
 *   <li>挡得住：并发连点、网络重试、双击提交——请求还在处理中时，后来的同内容请求直接被拒绝；</li>
 *   <li>挡不住：间隔提交。第一次已经成功返回、用户过一会儿又问一遍同样的内容，这是正常业务，
 *       不该拦，也没法只靠"内容相同"判断该不该拦；</li>
 *   <li>只在同一个实例内生效。键存在本机 Caffeine 里，多实例部署时两个副本各拦各的，
 *       真要全局兜住得把 {@link IdempotentAspect} 的存储换成 Redis（键加 {@code setIfAbsent}）。</li>
 * </ul>
 *
 * <p><b>为什么不用现有的 {@code DistributedLock}</b>：本地降级实现是按哈希落到 64 个分段锁上的，
 * 不同问题有可能落到同一段而互相阻塞（误撞），用「键 → 标记」的 CAS 语义更贴合去重这件事本身。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 重复提交时返回给前端的提示。 */
    String message() default "相同的请求正在处理中，请勿重复提交";
}
