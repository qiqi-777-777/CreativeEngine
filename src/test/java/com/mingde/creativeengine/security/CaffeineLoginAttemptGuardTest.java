package com.mingde.creativeengine.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进程内登录失败计数测试：阈值、清零、窗口过期、账号之间互不影响。
 */
class CaffeineLoginAttemptGuardTest {

    @Test
    void notBlockedBeforeThreshold() {
        LoginAttemptGuard guard = guard(Duration.ofMinutes(10), 3);

        guard.recordFailure("alice");
        guard.recordFailure("alice");

        assertFalse(guard.isBlocked("alice"));
    }

    @Test
    void blockedOnceThresholdReached() {
        LoginAttemptGuard guard = guard(Duration.ofMinutes(10), 3);

        guard.recordFailure("alice");
        guard.recordFailure("alice");
        guard.recordFailure("alice");

        assertTrue(guard.isBlocked("alice"));
    }

    @Test
    void resetUnlocksAccount() {
        LoginAttemptGuard guard = guard(Duration.ofMinutes(10), 2);
        guard.recordFailure("alice");
        guard.recordFailure("alice");
        assertTrue(guard.isBlocked("alice"));

        guard.reset("alice");

        assertFalse(guard.isBlocked("alice"), "登录成功后必须清零，否则正常用户会被自己锁死");
    }

    @Test
    void blockExpiresAfterWindow() throws Exception {
        LoginAttemptGuard guard = guard(Duration.ofMillis(100), 1);
        guard.recordFailure("alice");
        assertTrue(guard.isBlocked("alice"));

        Thread.sleep(300);

        assertFalse(guard.isBlocked("alice"), "窗口过期后应自动解锁");
    }

    @Test
    void countsAreIsolatedPerAccount() {
        LoginAttemptGuard guard = guard(Duration.ofMinutes(10), 2);
        guard.recordFailure("alice");
        guard.recordFailure("alice");

        assertTrue(guard.isBlocked("alice"));
        assertFalse(guard.isBlocked("bob"), "一个账号被锁不应影响其他账号");
    }

    private static LoginAttemptGuard guard(Duration window, int maxAttempts) {
        return new CaffeineLoginAttemptGuard(window, maxAttempts);
    }
}
