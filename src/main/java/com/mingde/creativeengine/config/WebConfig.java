package com.mingde.creativeengine.config;

import com.mingde.creativeengine.security.AdminInterceptor;
import com.mingde.creativeengine.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.List;

/**
 * Web配置
 *
 * @author CreativeEngine Team
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 免登录接口白名单。
     *
     * <p>与改造前保持一致：这些接口本来就是公开的（登录注册、首页 banner、政策与职业数据、
     * 小程序 AI 助手、BP 导出）。除此之外的 {@code /api/**} 一律需要登录，
     * 顺带补上了原先漏校验的 {@code /api/banner/admin/**} 与 {@code /api/agent/**}。</p>
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/banner/list",
            "/api/occupation/**",
            "/api/policy-data/**",
            "/api/bp/export/**",
            "/api/ai/**",
            "/api/test"
    };

    /**
     * 管理接口路径：命中即要求 ADMIN 角色。
     *
     * <p>用路径规则而不是「在方法上打注解」，是因为注解漏标一个方法就等于放行（默认允许）；
     * 路径规则是命中即校验（默认拒绝），新增接口只要落在这些前缀下就自动受保护。
     * 代价是新增别的管理路径要同步加到这里，{@code AdminPathCoverageTest}
     * 会扫描全部 Controller 来保证没有漏网的管理接口。</p>
     */
    public static final List<String> ADMIN_PATHS = List.of("/api/banner/admin/**");

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, AdminInterceptor adminInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    /**
     * 注册拦截器。顺序不能反：AdminInterceptor 依赖 AuthInterceptor 写进 UserContext 的 userId。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(PUBLIC_PATHS)
                .order(1);
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(ADMIN_PATHS)
                .order(2);
    }

    /**
     * 添加静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        String projectPath = System.getProperty("user.dir");
        String uploadPath = "file:" + projectPath + File.separator + "uploads" + File.separator;

        // 配置上传文件的访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
