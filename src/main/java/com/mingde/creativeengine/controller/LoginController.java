package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录控制器
 */
@RestController
@RequestMapping("/api/auth")
public class LoginController {
    
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    
    @Autowired
    private UserService userService;
    
    // 简单的token存储（生产环境建议使用Redis + JWT）
    private static final Map<String, Long> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = params.get("username");
            String password = params.get("password");
            
            // 参数校验
            if (username == null || username.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "用户名不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "密码不能为空");
                return result;
            }
            
            // 验证登录
            User user = userService.login(username, password);
            
            if (user == null) {
                result.put("code", 401);
                result.put("message", "用户名或密码错误");
                return result;
            }
            
            // 生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenStore.put(token, user.getId());
            
            // 返回用户信息和token
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userInfo", user);
            
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("data", data);
            
            log.info("用户登录成功: {}, token: {}", username, token);
            
        } catch (Exception e) {
            log.error("登录失败", e);
            result.put("code", 500);
            result.put("message", "登录失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = params.get("username");
            String password = params.get("password");
            String nickname = params.get("nickname");
            
            // 参数校验
            if (username == null || username.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "用户名不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "密码不能为空");
                return result;
            }
            
            if (username.length() < 3 || username.length() > 20) {
                result.put("code", 400);
                result.put("message", "用户名长度必须在3-20个字符之间");
                return result;
            }
            
            if (password.length() < 6) {
                result.put("code", 400);
                result.put("message", "密码长度不能少于6位");
                return result;
            }
            
            // 注册用户
            User user = userService.register(username, password, nickname);
            
            if (user == null) {
                result.put("code", 400);
                result.put("message", "用户名已存在");
                return result;
            }
            
            // 自动登录，生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenStore.put(token, user.getId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userInfo", user);
            
            result.put("code", 200);
            result.put("message", "注册成功");
            result.put("data", data);
            
            log.info("用户注册成功: {}", username);
            
        } catch (Exception e) {
            log.error("注册失败", e);
            result.put("code", 500);
            result.put("message", "注册失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (token != null && !token.isEmpty()) {
                tokenStore.remove(token);
                log.info("用户退出登录: token={}", token);
            }
            
            result.put("code", 200);
            result.put("message", "退出成功");
            
        } catch (Exception e) {
            log.error("退出登录失败", e);
            result.put("code", 500);
            result.put("message", "退出失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/userInfo")
    public Map<String, Object> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (token == null || token.isEmpty()) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            
            Long userId = tokenStore.get(token);
            if (userId == null) {
                result.put("code", 401);
                result.put("message", "登录已过期");
                return result;
            }
            
            User user = userService.getUserById(userId);
            if (user == null) {
                result.put("code", 404);
                result.put("message", "用户不存在");
                return result;
            }
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", user);
            
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            result.put("code", 500);
            result.put("message", "获取用户信息失败：" + e.getMessage());
        }
        
        return result;
    }
}
