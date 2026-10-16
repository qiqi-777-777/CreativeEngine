package com.mingde.creativeengine.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色解析测试：重点是「认不出来就当普通用户」，权限判断必须失败即最小权限。
 */
class UserRoleTest {

    @Test
    void parsesKnownRolesIgnoringCaseAndSpaces() {
        assertEquals(UserRole.ADMIN, UserRole.of("ADMIN"));
        assertEquals(UserRole.ADMIN, UserRole.of("admin"));
        assertEquals(UserRole.ADMIN, UserRole.of("  Admin  "));
        assertEquals(UserRole.USER, UserRole.of("USER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "ROOT", "1"})
    void unknownRoleFallsBackToNormalUser(String value) {
        assertEquals(UserRole.USER, UserRole.of(value), "无法识别的角色必须是普通用户，不能放行");
    }

    @Test
    void blankOrNullRoleFallsBackToNormalUser() {
        assertEquals(UserRole.USER, UserRole.of(null));
        assertEquals(UserRole.USER, UserRole.of(""));
        assertEquals(UserRole.USER, UserRole.of("   "));
    }

    @Test
    void onlyAdminIsAdmin() {
        assertTrue(UserRole.ADMIN.isAdmin());
        assertFalse(UserRole.USER.isAdmin());
    }
}
