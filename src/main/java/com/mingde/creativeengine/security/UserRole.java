package com.mingde.creativeengine.security;

import java.util.Locale;

/**
 * 用户角色。
 *
 * <p>只有两级：普通用户只能访问自己的数据；管理员额外可以访问 {@code /admin} 管理接口。
 * 需要更细的权限时再引入权限点表，现在没必要提前设计。</p>
 */
public enum UserRole {

    /** 普通用户。 */
    USER,

    /** 管理员。 */
    ADMIN;

    /**
     * 把库里的字符串解析成角色。
     *
     * <p>大小写不敏感；空值或无法识别的值一律按 {@link #USER} 处理——
     * 权限判断要「失败即最小权限」，而不是抛异常把请求打挂或放行。</p>
     */
    public static UserRole of(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
