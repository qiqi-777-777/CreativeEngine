package com.mingde.creativeengine.aop;

import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.exception.BusinessException;
import com.mingde.creativeengine.security.UserContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 接口幂等切面测试。
 *
 * <p>用真实的 Spring AOP 代理跑，这样「并发时是否真的互相看得见标记」「异常后标记有没有释放」
 * 这些行为才和线上一致。需要并发的用例通过一个闸门把第一条请求卡在方法体里模拟"模型还在算"。</p>
 */
class IdempotentAspectTest {

    private AnnotationConfigApplicationContext context;
    private ProbeService probe;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.registerBean(IdempotentAspect.class, IdempotentAspect::new);
        context.registerBean(ProbeService.class, ProbeService::new);
        context.register(AopConfig.class);
        context.refresh();

        probe = context.getBean(ProbeService.class);
        UserContext.set(7L);
    }

    @AfterEach
    void tearDown() {
        probe.release();
        UserContext.clear();
        context.close();
    }

    @Test
    void concurrentDuplicateIsRejectedAndNeverReachesTheMethod() throws Exception {
        probe.blockNextCall();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = pool.submit(() -> asUser(7L, () -> probe.ask("应届生补贴")));
            assertTrue(probe.awaitEntered(3, TimeUnit.SECONDS), "第一条请求应已进入方法体");

            BusinessException ex = assertThrows(BusinessException.class, () -> probe.ask("应届生补贴"));
            assertEquals(ResultCode.OPERATION_FAILED.getCode(), ex.getCode());
            assertEquals(1, probe.calls(), "被拦下的重复请求不能再触发一次模型调用");

            probe.release();
            assertEquals("答案：应届生补贴", first.get(3, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void markerIsReleasedAfterSuccess() {
        assertEquals("答案：应届生补贴", probe.ask("应届生补贴"));

        assertEquals("答案：应届生补贴", probe.ask("应届生补贴"), "前一次已返回，用户再问一遍同样的内容不应被拦");
    }

    @Test
    void markerIsReleasedAfterFailure() {
        assertThrows(IllegalStateException.class, () -> probe.fail("问题"));

        // 第二次抛的是业务方法自己的异常，而不是"重复提交"——说明标记已被释放
        assertThrows(IllegalStateException.class, () -> probe.fail("问题"),
                "首次调用失败后必须释放标记，否则同一个请求永远被拦成重复提交");
    }

    @Test
    void differentArgumentsAreNotBlocked() throws Exception {
        probe.blockNextCall();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = pool.submit(() -> asUser(7L, () -> probe.ask("问题 A")));
            assertTrue(probe.awaitEntered(3, TimeUnit.SECONDS), "第一条请求应已进入方法体");

            assertEquals("答案：问题 B", probe.ask("问题 B"), "内容不同的请求应各走各的");
            probe.release();
            first.get(3, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sameContentFromAnotherUserIsNotBlocked() throws Exception {
        probe.blockNextCall();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = pool.submit(() -> asUser(7L, () -> probe.ask("热门问题")));
            assertTrue(probe.awaitEntered(3, TimeUnit.SECONDS), "第一条请求应已进入方法体");

            assertEquals("答案：热门问题", asUser(8L, () -> probe.ask("热门问题")),
                    "换个用户问同样的问题，不该被别人的在途请求牵连");
            probe.release();
            first.get(3, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void unannotatedMethodIsNotGuarded() {
        probe.notAnnotated("问题");
        probe.notAnnotated("问题");

        assertEquals(2, probe.calls());
    }

    /** UserContext 是 ThreadLocal，跑在别的线程上时得自己设一遍。 */
    private static <T> T asUser(Long userId, java.util.concurrent.Callable<T> action) {
        UserContext.set(userId);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            UserContext.clear();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AopConfig {
    }

    /**
     * 探针 Bean：{@code blockNextCall()} 之后，紧接着的那一次 {@code ask} 会卡在方法体里等人放行，
     * 用来模拟"模型还在算"的在途状态。
     */
    public static class ProbeService {

        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean gateArmed = new AtomicBoolean(false);
        private volatile CountDownLatch entered = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);

        void blockNextCall() {
            entered = new CountDownLatch(1);
            release = new CountDownLatch(1);
            gateArmed.set(true);
        }

        boolean awaitEntered(long timeout, TimeUnit unit) throws InterruptedException {
            return entered.await(timeout, unit);
        }

        void release() {
            release.countDown();
        }

        int calls() {
            return calls.get();
        }

        @Idempotent
        public String ask(String question) {
            calls.incrementAndGet();
            if (gateArmed.compareAndSet(true, false)) {
                entered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "答案：" + question;
        }

        @Idempotent(message = "上一个请求还没算完")
        public String fail(String question) {
            calls.incrementAndGet();
            throw new IllegalStateException("模型挂了");
        }

        public String notAnnotated(String question) {
            calls.incrementAndGet();
            return "no-guard";
        }
    }
}
