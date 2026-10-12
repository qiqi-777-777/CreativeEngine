package com.mingde.creativeengine.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个请求建立 traceId 上下文。
 *
 * <p>放在过滤器而不是 Controller 里，是为了让所有入口（包括不经过 Agent 的接口）
 * 都有一致的 traceId，并且一定在 finally 中清理，避免线程复用导致串号。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceContext.normalize(request.getHeader(TraceContext.TRACE_ID_HEADER));
        TraceContext.begin(traceId);
        // 回写响应头，便于前端在报错时把 traceId 一起反馈
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
