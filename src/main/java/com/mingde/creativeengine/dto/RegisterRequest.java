package com.mingde.creativeengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码长度不能少于6位")
        String password,

        String nickname) {
}
