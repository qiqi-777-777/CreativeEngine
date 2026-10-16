package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.dto.LoginRequest;
import com.mingde.creativeengine.dto.RegisterRequest;
import com.mingde.creativeengine.dto.UpdateUserProfileRequest;
import com.mingde.creativeengine.entity.User;
import com.mingde.creativeengine.security.UserContext;
import com.mingde.creativeengine.service.UserService;
import com.mingde.creativeengine.storage.TokenStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 登录控制器
 */
@Tag(name = "登录与账号", description = "登录、注册、退出与个人资料")
@RestController
@RequestMapping("/api/auth")
public class LoginController {
    
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    
    @Autowired
    private UserService userService;

    // 登录 token 存储：默认单机内存，开启 Redis 后多实例共享并自动过期
    @Autowired
    private TokenStore tokenStore;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 必填校验交给 @Valid + LoginRequest，方法体只关心业务
            User user = userService.login(request.username(), request.password());
            
            if (user == null) {
                result.put("code", 401);
                result.put("message", "用户名或密码错误");
                return result;
            }
            
            // 生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenStore.save(token, user.getId());
            
            // 返回用户信息和token
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userInfo", user);
            
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("data", data);
            
            log.info("用户登录成功: {}, token: {}", request.username(), maskToken(token));
            
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
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 用户名/密码的必填与长度规则写在 RegisterRequest 上
            User user = userService.register(request.username(), request.password(), request.nickname());
            
            if (user == null) {
                result.put("code", 400);
                result.put("message", "用户名已存在");
                return result;
            }
            
            // 自动登录，生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenStore.save(token, user.getId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userInfo", user);
            
            result.put("code", 200);
            result.put("message", "注册成功");
            result.put("data", data);
            
            log.info("用户注册成功: {}", request.username());
            
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
                tokenStore.revoke(token);
                log.info("用户退出登录: token={}", maskToken(token));
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
    public Map<String, Object> getUserInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 登录态已由 AuthInterceptor 统一校验，这里直接取
            Long userId = UserContext.getUserId();

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

    @PutMapping("/userInfo")
    public Map<String, Object> updateUserInfo(@Valid @RequestBody UpdateUserProfileRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 登录态已由 AuthInterceptor 统一校验，这里直接取
            Long userId = UserContext.getUserId();

            // 昵称必填、手机号与邮箱格式规则写在 UpdateUserProfileRequest 上
            User user = userService.updateUserInfo(userId, request.nickname(), request.phone(),
                    request.email(), request.avatar());
            if (user == null) {
                result.put("code", 404);
                result.put("message", "用户不存在");
                return result;
            }

            result.put("code", 200);
            result.put("message", "保存成功");
            result.put("data", user);
        } catch (Exception e) {
            log.error("更新用户资料失败", e);
            result.put("code", 500);
            result.put("message", "更新用户资料失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * token 脱敏后再落日志，避免日志文件泄露可直接冒用的登录凭证。
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
