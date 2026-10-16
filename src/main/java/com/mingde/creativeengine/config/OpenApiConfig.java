package com.mingde.creativeengine.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置。
 *
 * <p>启动后访问 {@code /swagger-ui.html}，OpenAPI 描述在 {@code /v3/api-docs}。
 * 这两条路径不在 {@code /api/**} 下，因此不会被 AuthInterceptor 拦截。</p>
 *
 * <p>除白名单外的 {@code /api/**} 都需要登录，这里声明 Authorization 头的安全方案，
 * 在 Swagger UI 里点「Authorize」填入登录接口返回的 token 即可直接调试受保护接口。</p>
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "创享引擎后端 API",
        version = "1.0",
        description = "政策解读、职业规划、BP 写作、创业咨询等场景化 Agent 的接口说明"))
@SecurityScheme(
        name = "Authorization",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "Authorization",
        description = "登录接口返回的 token，填 token 原值或 Bearer <token> 都可以")
public class OpenApiConfig {
}
