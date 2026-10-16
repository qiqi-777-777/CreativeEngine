package com.mingde.creativeengine.service.impl;

import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.exception.BusinessException;
import com.mingde.creativeengine.mapper.UserMapper;
import com.mingde.creativeengine.security.CaffeineLoginAttemptGuard;
import com.mingde.creativeengine.security.LoginAttemptGuard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 密码存储测试：新密码必须是 BCrypt，存量 MD5 密码仍能登录并在登录时自动升级。
 */
class UserServiceImplTest {

    private static final int MAX_ATTEMPTS = 3;

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl();
    private final LoginAttemptGuard loginAttemptGuard =
            new CaffeineLoginAttemptGuard(Duration.ofMinutes(10), MAX_ATTEMPTS);

    UserServiceImplTest() {
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(service, "loginAttemptGuard", loginAttemptGuard);
    }

    @Test
    void blocksAccountAfterRepeatedFailures() {
        // 用 thenAnswer 而不是 thenReturn：login 成功后会清空返回对象的密码字段，
        // 若共用同一个实例，后续调用拿到的 password 就是 null，测试会失真
        when(userMapper.selectOne(any())).thenAnswer(invocation -> bcryptUser(1L, "alice", "secret123"));

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertNull(service.login("alice", "wrong-password"));
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login("alice", "secret123"));
        assertTrue(exception.getMessage().contains("稍后再试"), "锁定期间即使密码正确也要拒绝：" + exception.getMessage());
        verify(userMapper, times(MAX_ATTEMPTS)).selectOne(any());
    }

    @Test
    void unknownAccountAlsoCountsAsFailure() {
        when(userMapper.selectOne(any())).thenReturn(null);

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertNull(service.login("ghost", "whatever"));
        }

        assertThrows(BusinessException.class, () -> service.login("ghost", "whatever"),
                "不存在的账号同样计数，否则可以靠是否被限流反推账号是否存在");
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        when(userMapper.selectOne(any())).thenAnswer(invocation -> bcryptUser(1L, "alice", "secret123"));

        assertNull(service.login("alice", "wrong-password"));
        assertNull(service.login("alice", "wrong-password"));
        assertNotNull(service.login("alice", "secret123"));

        // 清零后重新计数：还能再失败两次
        assertNull(service.login("alice", "wrong-password"));
        assertNull(service.login("alice", "wrong-password"));
        assertNotNull(service.login("alice", "secret123"), "登录成功清零后不应被自己锁死");
    }

    @Test
    void registerStoresBcryptHash() {
        when(userMapper.selectOne(any())).thenReturn(null);
        // 注册成功后会把返回对象的密码字段清空，所以在 insert 调用的当下就把哈希取出来
        AtomicReference<String> storedHash = new AtomicReference<>();
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            storedHash.set(((User) invocation.getArgument(0)).getPassword());
            return 1;
        });

        User created = service.register("alice", "secret123", "Alice");

        assertNotNull(created);
        assertNull(created.getPassword(), "返回给上层的用户对象不应带密码");
        String stored = storedHash.get();
        assertNotNull(stored);
        assertTrue(stored.startsWith("$2"), "新密码必须是 BCrypt，实际：" + stored);
        assertTrue(stored.length() >= 60, "BCrypt 哈希长度为 60 位");
    }

    @Test
    void loginAcceptsBcryptPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", new BCryptPasswordEncoder().encode("secret123")));

        assertNotNull(service.login("alice", "secret123"));
    }

    @Test
    void loginRejectsWrongPasswordWithoutTouchingDatabase() {
        when(userMapper.selectOne(any())).thenReturn(
                user(1L, "alice", new BCryptPasswordEncoder().encode("secret123")));

        assertNull(service.login("alice", "wrong-password"));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void loginAcceptsLegacyMd5AndUpgradesItToBcrypt() {
        when(userMapper.selectOne(any())).thenReturn(user(5L, "bob", md5Hex("secret123")));

        User logged = service.login("bob", "secret123");

        assertNotNull(logged, "存量 MD5 密码的用户必须还能登录");
        assertNull(logged.getPassword(), "返回给上层的用户对象不应带密码");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User update = captor.getValue();
        assertEquals(5L, update.getId());
        assertTrue(update.getPassword().startsWith("$2"), "登录成功后应把 MD5 无感升级为 BCrypt");
    }

    @Test
    void loginRejectsLegacyMd5WithWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(5L, "bob", md5Hex("secret123")));

        assertNull(service.login("bob", "wrong-password"));
        verify(userMapper, never()).updateById(any(User.class));
    }

    private User user(Long id, String username, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setStatus(1);
        return user;
    }

    /** 造一个密码已是 BCrypt 的用户，走正常校验路径。 */
    private User bcryptUser(Long id, String username, String rawPassword) {
        return user(id, username, new BCryptPasswordEncoder().encode(rawPassword));
    }

    /** 复现改造前的 MD5 哈希，用于构造存量数据。 */
    private static String md5Hex(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
