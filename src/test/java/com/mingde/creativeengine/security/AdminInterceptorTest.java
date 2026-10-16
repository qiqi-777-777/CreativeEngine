package com.mingde.creativeengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员接口鉴权测试：普通用户、未登录、用户不存在、角色无法识别，四种情况都必须 403。
 */
class AdminInterceptorTest {

    private final UserService userService = mock(UserService.class);
    private final AdminInterceptor interceptor = new AdminInterceptor(userService, new ObjectMapper());

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void rejectsWhenNobodyLoggedIn() throws Exception {
        HttpServletResponse response = forbiddenResponse();

        assertFalse(interceptor.preHandle(request(), response, new Object()));
        verify(response).setStatus(403);
        verify(userService, never()).getUserById(any());
    }

    @Test
    void rejectsNormalUser() throws Exception {
        UserContext.set(9L);
        when(userService.getUserById(9L)).thenReturn(user("USER"));
        HttpServletResponse response = forbiddenResponse();
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        assertFalse(interceptor.preHandle(request(), response, new Object()));
        verify(response).setStatus(403);
        assertTrue(body.toString().contains("2005"), "响应体应是 PERMISSION_DENIED：" + body);
    }

    @Test
    void rejectsUnknownRole() throws Exception {
        UserContext.set(9L);
        when(userService.getUserById(9L)).thenReturn(user("SUPER"));

        assertFalse(interceptor.preHandle(request(), forbiddenResponse(), new Object()));
    }

    @Test
    void rejectsWhenUserNotFound() throws Exception {
        UserContext.set(9L);
        when(userService.getUserById(9L)).thenReturn(null);

        assertFalse(interceptor.preHandle(request(), forbiddenResponse(), new Object()));
    }

    @Test
    void allowsAdmin() throws Exception {
        UserContext.set(9L);
        when(userService.getUserById(9L)).thenReturn(user("ADMIN"));
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertTrue(interceptor.preHandle(request(), response, new Object()));
        verify(response, never()).setStatus(anyInt());
    }

    private HttpServletResponse forbiddenResponse() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/banner/admin/list");
        return request;
    }

    private static User user(String role) {
        User user = new User();
        user.setId(9L);
        user.setUsername("tester");
        user.setRole(role);
        return user;
    }
}
