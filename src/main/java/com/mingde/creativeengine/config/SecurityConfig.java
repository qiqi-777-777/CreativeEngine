package com.mingde.creativeengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关的通用 Bean。
 *
 * <p>单独放一个配置类而不是塞进 {@code WebConfig}：{@code WebConfig} 要依赖
 * {@code AdminInterceptor} → {@code UserService} → {@code PasswordEncoder}，
 * 而 {@code @Bean} 方法所属的配置类本身也算在依赖链里，
 * 放在 WebConfig 会直接构成循环依赖（启动即失败）。</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码加密器：BCrypt 每次加密都带随机盐，同一明文两次结果不同，
     * 彩虹表与相同口令比对这两类攻击都失效。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
