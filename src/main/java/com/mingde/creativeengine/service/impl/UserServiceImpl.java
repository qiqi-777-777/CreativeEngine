package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.mapper.UserMapper;
import com.mingde.creativeengine.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public User login(String username, String password) {
        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            log.warn("用户不存在: {}", username);
            return null;
        }
        
        if (user.getStatus() == 0) {
            log.warn("用户已被禁用: {}", username);
            return null;
        }
        
        // 验证密码
        String encryptedPassword = encryptPassword(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            log.warn("密码错误: {}", username);
            return null;
        }
        
        // 清除密码字段
        user.setPassword(null);
        
        log.info("用户登录成功: {}", username);
        return user;
    }
    
    @Override
    public User register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User existUser = userMapper.selectOne(queryWrapper);
        
        if (existUser != null) {
            log.warn("用户名已存在: {}", username);
            return null;
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : username);
        user.setStatus(1);
        
        int result = userMapper.insert(user);
        
        if (result > 0) {
            log.info("用户注册成功: {}", username);
            user.setPassword(null);
            return user;
        }
        
        return null;
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
    
    /**
     * 密码加密（使用MD5，生产环境建议使用BCrypt）
     */
    private String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
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
            log.error("密码加密失败", e);
            return password;
        }
    }
}
