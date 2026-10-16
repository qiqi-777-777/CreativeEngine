package com.mingde.creativeengine.config;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.PathContainer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理接口覆盖测试。
 *
 * <p>{@link WebConfig#ADMIN_PATHS} 是「命中即校验」的白名单，好处是不会漏校验，
 * 代价是新增管理路径时可能忘了同步——那就退化成普通用户可访问。
 * 这个测试扫描所有 Controller 的实际映射路径，把所有带 {@code /admin/} 的接口
 * 与规则做匹配，确保没有漏网的。</p>
 */
@SpringBootTest
class AdminPathCoverageTest {

    @Autowired
    private ApplicationContext context;

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
