package com.mingde.creativeengine.service.impl;

import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 密码存储测试：新密码必须是 BCrypt，存量 MD5 密码仍能登录并在登录时自动升级。
 */
class UserServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl();

    UserServiceImplTest() {
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());
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
