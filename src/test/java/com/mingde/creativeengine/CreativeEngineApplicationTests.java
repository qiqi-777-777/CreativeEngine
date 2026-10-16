package com.mingde.creativeengine;

import com.mingde.creativeengine.aop.AgentCallLogAspect;
import com.mingde.creativeengine.config.WebConfig;
import com.mingde.creativeengine.controller.AgentController;
import com.mingde.creativeengine.mq.AgentLogPublisher;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.PathContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用装配校验：启动完整上下文，把「注解写了但没生效」这类问题固定在测试里。
 *
 * <p>用 Testcontainers 提供数据库，而不是依赖「本机跑着一个 MySQL」——
 * 后者会让 {@code mvn test} 的结果取决于开发机的环境，CI 上直接跑不起来。</p>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CreativeEngineApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("creative_engine");

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    /**
     * {@code @Transactional} 依赖事务管理器：少了它，注解写了也不生效，而且要到运行期调用时才报错。
     */
    @Test
    void transactionManagerIsAvailable() {
        assertNotNull(context.getBean(PlatformTransactionManager.class),
                "缺少事务管理器时 @Transactional 会在运行期才报错");
    }

    /**
     * 切面最容易「静默失效」：少了 aspectjweaver、切点写错、Bean 没被扫到，
     * 应用照样能正常启动，只是日志悄悄不再写了。这里把装配关系固定住。
     */
    @Test
    void agentControllerIsProxiedByCallLogAspect() {
        assertNotNull(context.getBean(AgentLogPublisher.class), "日志投递入口应已装配");
        assertEquals(1, context.getBeansOfType(AgentCallLogAspect.class).size(), "应存在唯一的 Agent 日志切面");

        AgentController controller = context.getBean(AgentController.class);
        assertTrue(AopUtils.isAopProxy(controller), "AgentController 应被代理，否则 @AgentCallLog 不会生效");
    }

    /**
     * 管理接口覆盖检查。
     *
     * <p>{@link WebConfig#ADMIN_PATHS} 是「命中即校验」的白名单，好处是不会漏校验，
     * 代价是新增管理路径时可能忘了同步——那就退化成普通用户可访问。
     * 这里扫描所有 Controller 的实际映射路径，确保没有漏网的管理接口。</p>
     */
    @Test
    void everyAdminEndpointIsCoveredByAdminPaths() {
        List<PathPattern> guarded = WebConfig.ADMIN_PATHS.stream()
                .map(new PathPatternParser()::parse)
                .toList();

        List<String> unguarded = new ArrayList<>();
        for (Object bean : context.getBeansWithAnnotation(RestController.class).values()) {
            for (String path : declaredPaths(AopUtils.getTargetClass(bean))) {
                if (path.contains("/admin/") && notGuarded(guarded, path)) {
                    unguarded.add(path);
                }
            }
        }

        assertTrue(unguarded.isEmpty(), "以下管理接口不在 ADMIN_PATHS 里，普通用户即可访问：" + unguarded);
    }

    private static boolean notGuarded(List<PathPattern> guarded, String path) {
        return guarded.stream().noneMatch(pattern -> pattern.matches(PathContainer.parsePath(path)));
    }

    private static List<String> declaredPaths(Class<?> controller) {
        List<String> prefixes = mappingsOf(controller.getAnnotations());
        if (prefixes.isEmpty()) {
            prefixes = List.of("");
        }

        List<String> paths = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            for (String mapping : mappingsOf(method.getAnnotations())) {
                for (String prefix : prefixes) {
                    paths.add(concretize(join(prefix, mapping)));
                }
            }
        }
        return paths;
    }

    /** 取出 @RequestMapping / @GetMapping / @PostMapping … 上声明的路径。 */
    private static List<String> mappingsOf(Annotation[] annotations) {
        List<String> mappings = new ArrayList<>();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            // 各种 *Mapping 都是被 @RequestMapping 元注解的
            if (type != RequestMapping.class && !type.isAnnotationPresent(RequestMapping.class)) {
                continue;
            }
            try {
                Collections.addAll(mappings, (String[]) type.getMethod("value").invoke(annotation));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("读取 " + type.getSimpleName() + " 的路径失败", e);
            }
        }
        return mappings;
    }

    private static String join(String prefix, String mapping) {
        String left = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String right = mapping.startsWith("/") ? mapping : "/" + mapping;
        return left + right;
    }

    /** PathPattern.matches 只比较具体路径，这里把 {id} 之类的占位符换成实际值。 */
    private static String concretize(String path) {
        return path.replaceAll("\\{[^}]+}", "1");
    }
}
