package com.mingde.creativeengine.config;

import com.mingde.creativeengine.security.AuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

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

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 密码加密器：BCrypt 每次加密都会带上随机盐，同一明文两次结果不同，彩虹表与相同口令比对的攻击都失效。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 注册统一登录校验拦截器。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(PUBLIC_PATHS);
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
