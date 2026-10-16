package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.exception.BusinessException;
import com.mingde.creativeengine.mapper.UserMapper;
import com.mingde.creativeengine.security.LoginAttemptGuard;
import com.mingde.creativeengine.security.UserRole;
import com.mingde.creativeengine.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 用户服务实现类
 *
 * <p><b>事务约定</b>：服务层的写方法默认加 {@code @Transactional(rollbackFor = Exception.class)}。
 * 目前这些方法都只写一张表，事务还看不出效果；但一旦以后出现「注册用户 + 初始化偏好」这类多表写，
 * 就不会因为漏加注解留下半截数据。显式写 {@code rollbackFor} 的原因：默认只回滚
 * {@link RuntimeException}，受检异常不会回滚。</p>
 *
 * <p><b>为什么 {@link #login} 刻意不加事务</b>：BCrypt 校验是故意设计成慢操作的
 * （几十到上百毫秒），把它包在事务里会一直占着数据库连接，并发登录时容易把连接池耗光。
 * 登录里唯一的写是「存量 MD5 升级为 BCrypt」，单条 UPDATE 自带原子性，不需要事务。</p>
 */
@Service
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptGuard loginAttemptGuard;
    
    @Override
    public User login(String username, String password) {
        // 已被锁定就不再查库：暴力尝试继续消耗资源，而且失败原因也不必细究
        if (loginAttemptGuard.isBlocked(username)) {
            log.warn("登录失败次数过多，账号临时锁定: {}", username);
            throw new BusinessException(ResultCode.PERMISSION_DENIED.getCode(), "登录失败次数过多，请稍后再试");
        }

        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            // 账号不存在也计数：否则可以靠"是否被限流"反推某个账号是否存在
            recordLoginFailure(username);
            log.warn("用户不存在: {}", username);
            return null;
        }
        
        if (user.getStatus() == 0) {
            // 禁用不是凭证错误，不计数
            log.warn("用户已被禁用: {}", username);
            return null;
        }
        
        // 验证密码
        if (!matchesPassword(user, password)) {
            recordLoginFailure(username);
            log.warn("密码错误: {}", username);
            return null;
        }

        loginAttemptGuard.reset(username);

        // 存量用户库里还是 MD5（无盐），校验通过后顺手升级为 BCrypt，用户无感
        if (isLegacyMd5(user.getPassword())) {
            upgradeToBcrypt(user.getId(), password);
        }
        
        // 清除密码字段
        user.setPassword(null);
        
        log.info("用户登录成功: {}", username);
        return user;
    }

    private void recordLoginFailure(String username) {
        loginAttemptGuard.recordFailure(username);
        if (loginAttemptGuard.isBlocked(username)) {
            log.warn("账号 {} 连续登录失败达 {} 次，已锁定 {}",
                    username, loginAttemptGuard.maxAttempts(), loginAttemptGuard.window());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(String username, String password, String nickname) {
        // 先查一次是为了给出友好的「用户名已存在」；
        // 真正的并发兜底是 users 表上的 uk_username 唯一索引（见下面 catch）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        if (userMapper.selectOne(queryWrapper) != null) {
            log.warn("用户名已存在: {}", username);
            return null;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : username);
        user.setStatus(1);
        // 角色由服务端固定为普通用户：注册接口不允许客户端指定，否则就是提权漏洞
        user.setRole(UserRole.USER.name());

        try {
            if (userMapper.insert(user) <= 0) {
                log.warn("用户写入未生效: {}", username);
                return null;
            }
        } catch (DuplicateKeyException e) {
            // 并发提交同名账号时唯一索引会拦下，转成与「已存在」一致的返回，
            // 否则用户看到的是笼统的 5000 系统错误
            log.warn("用户名并发注册冲突: {}", username);
            return null;
        }

        log.info("用户注册成功: {}", username);
        user.setPassword(null);
        return user;
    }
    
    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setPassword(null);
        }
        
        return user;
    }
    
    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        
        if (user != null) {
            user.setPassword(null);
        }
        
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUserInfo(Long id, String nickname, String phone, String email, String avatar) {
        User user = userMapper.selectById(id);

        if (user == null) {
            return null;
        }

        if (nickname != null) {
            user.setNickname(nickname.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        if (email != null) {
            user.setEmail(email.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar.trim());
        }

        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
    
    /**
     * 校验密码，兼容两种存储格式：
     * <ul>
     *   <li>新密码：BCrypt（60 位、带随机盐、慢哈希），走 {@link PasswordEncoder#matches}；</li>
     *   <li>存量密码：32 位十六进制 MD5，仅用于老账号过渡。</li>
     * </ul>
     */
    private boolean matchesPassword(User user, String rawPassword) {
        String stored = user.getPassword();
        if (isLegacyMd5(stored)) {
            return stored.equalsIgnoreCase(md5Hex(rawPassword));
        }
        return passwordEncoder.matches(rawPassword, stored);
    }

    /**
     * 判断是否为改造前遗留的 MD5 哈希：32 位且全部是十六进制字符。
     */
    private boolean isLegacyMd5(String hash) {
        if (hash == null || hash.length() != 32) {
            return false;
        }
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            boolean isHex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!isHex) {
                return false;
            }
        }
        return true;
    }

    /**
     * 存量 MD5 密码升级为 BCrypt。
     *
     * <p>只能在"密码已校验通过"的那一刻调用：MD5 无法反推出明文，也就无法离线批量重算，
     * 所以采用登录时渐进升级，用户全程无感。</p>
     */
    private void upgradeToBcrypt(Long userId, String rawPassword) {
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
        log.info("存量 MD5 密码已升级为 BCrypt，userId={}", userId);
    }

    /**
     * MD5 摘要，仅用于校验存量密码，新密码一律走 BCrypt。
     */
    private String md5Hex(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 计算失败", e);
            return password;
        }
    }
}
