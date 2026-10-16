package com.mingde.creativeengine.security;

/**
 * 当前请求的登录用户上下文。
 *
 * <p>由 {@link AuthInterceptor} 在请求进入时写入、请求结束时清理。
 * 业务代码只要拿到 {@link #getUserId()} 即可，不必再从每个方法里重复解析 token。</p>
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    /** 写入当前请求的登录用户 ID。 */
    public static void set(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 取当前请求的登录用户 ID，未登录时为 {@code null}。 */
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 请求结束后清理，避免线程复用把上一个请求的用户串到下一个请求。 */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
