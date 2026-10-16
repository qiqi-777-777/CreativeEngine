package com.mingde.creativeengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 更新个人资料请求。
 *
 * <p>手机号与邮箱允许留空（{@code null} 或空串），非空时才校验格式。</p>
 */
public record UpdateUserProfileRequest(
        @NotBlank(message = "昵称不能为空")
        String nickname,

        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "邮箱格式不正确")
        String email,

        String avatar) {
}
