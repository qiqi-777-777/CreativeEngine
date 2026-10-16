package com.mingde.creativeengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.common.Result;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 拦截器里直接返回错误响应的公共写法。
 *
 * <p>鉴权与鉴权失败都要回同样的结构（HTTP 状态码 + 业务码），抽出来免得两处各写一遍
 * 还写得不一样。</p>
 */
final class ApiErrorResponse {

    private ApiErrorResponse() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, int status, Result<Void> body)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
