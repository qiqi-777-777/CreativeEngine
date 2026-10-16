package com.mingde.creativeengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.storage.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一登录校验拦截器。
 *
 * <p>改造前是每个接口各自取 {@code Authorization} 头，再调 TokenStore 解析，带来三个问题：</p>
 * <ol>
 *   <li>重复代码散落在多个 Controller，新增受保护接口时容易漏写；</li>
 *   <li>{@code /api/agent/**} 部分接口在解析失败（userId 为 null）后仍然继续执行并写库；</li>
 *   <li>鉴权分散，无法统一兜底。</li>
 * </ol>
 *
 * <p>现在统一在请求进入时校验一次：通过则把 userId 放进 {@link UserContext}，
 * 否则直接返回 401（HTTP 状态码与响应体同时给出，前端按状态码跳登录即可）。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(TokenStore tokenStore, ObjectMapper objectMapper) {
        this.tokenStore = tokenStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求不带 Authorization，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // TokenStore 内部会剥离 "Bearer " 前缀
        Long userId = tokenStore.resolve(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (userId == null) {
            log.warn("未登录访问受保护接口：{} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
            return false;
        }

        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        ApiErrorResponse.write(response, objectMapper,
                HttpStatus.UNAUTHORIZED.value(), Result.error(ResultCode.TOKEN_INVALID));
    }
}
