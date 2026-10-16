package com.mingde.creativeengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.storage.InMemoryTokenStore;
import com.mingde.creativeengine.storage.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一鉴权拦截器测试：拦截失败必须返回 401 且不进入业务方法，成功则把 userId 放进 UserContext。
 */
class AuthInterceptorTest {

    private final TokenStore tokenStore = new InMemoryTokenStore(Duration.ofMinutes(10));
    private final AuthInterceptor interceptor = new AuthInterceptor(tokenStore, new ObjectMapper());

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean allowed = interceptor.preHandle(request("GET", "/api/agent/logs", null), response, new Object());

        assertFalse(allowed, "没有 token 必须拦截");
        verify(response).setStatus(401);
        assertTrue(body.toString().contains("2003"), "响应体应是 TOKEN_INVALID");
        assertNull(UserContext.getUserId(), "拦截后不应写入用户上下文");
    }

    @Test
    void rejectsRequestWithUnknownToken() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        boolean allowed = interceptor.preHandle(
                request("GET", "/api/user-data/stats", "Bearer not-exists"), response, new Object());

        assertFalse(allowed);
        verify(response).setStatus(401);
    }

    @Test
    void allowsRequestWithValidTokenAndExposesUserId() throws Exception {
        tokenStore.save("abc123", 7L);
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(
                request("GET", "/api/agent/logs", "Bearer abc123"), response, new Object());

        assertTrue(allowed);
        assertEquals(7L, UserContext.getUserId());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void allowsCorsPreflightWithoutToken() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(request("OPTIONS", "/api/agent/logs", null), response, new Object());

        assertTrue(allowed, "跨域预检不带 Authorization，必须放行");
    }

    @Test
    void clearsUserContextAfterCompletion() throws Exception {
        UserContext.set(99L);

        interceptor.afterCompletion(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object(), null);

        assertNull(UserContext.getUserId(), "请求结束必须清理，避免线程复用串号");
    }

    private HttpServletRequest request(String method, String uri, String authorization) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getHeader("Authorization")).thenReturn(authorization);
        return request;
    }
}
