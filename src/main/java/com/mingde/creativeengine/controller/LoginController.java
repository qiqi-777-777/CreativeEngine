package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
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
     *
     * <p>刻意不吞异常：登录被防爆破拦下时 {@code UserServiceImpl} 抛的是带业务码的
     * {@code BusinessException}，交给 {@link com.mingde.creativeengine.exception.GlobalExceptionHandler}
     * 才能原样透出 2005 而不是被裹成 5000。</p>
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        // 必填校验交给 @Valid + LoginRequest，方法体只关心业务
        User user = userService.login(request.username(), request.password());

        if (user == null) {
            return Result.error(ResultCode.LOGIN_FAILED);
        }

        // 生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.save(token, user.getId());

        log.info("用户登录成功: {}, token: {}", request.username(), maskToken(token));

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success(data);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // 用户名/密码的必填与长度规则写在 RegisterRequest 上
        User user = userService.register(request.username(), request.password(), request.nickname());

        if (user == null) {
            return Result.error(ResultCode.DATA_EXISTED.getCode(), "用户名已存在");
        }

        // 自动登录，生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.save(token, user.getId());

        log.info("用户注册成功: {}", request.username());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return Result.success(data);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            tokenStore.revoke(token);
            log.info("用户退出登录: token={}", maskToken(token));
        }
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userInfo")
    public Result<User> getUserInfo() {
        // 登录态已由 AuthInterceptor 统一校验，这里直接取
        User user = userService.getUserById(UserContext.getUserId());
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success(user);
    }

    @PutMapping("/userInfo")
    public Result<User> updateUserInfo(@Valid @RequestBody UpdateUserProfileRequest request) {
        // 昵称必填、手机号与邮箱格式规则写在 UpdateUserProfileRequest 上
        User user = userService.updateUserInfo(UserContext.getUserId(), request.nickname(),
                request.phone(), request.email(), request.avatar());
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        return Result.success(user);
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
