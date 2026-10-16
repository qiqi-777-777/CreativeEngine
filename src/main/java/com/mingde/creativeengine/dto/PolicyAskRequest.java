package com.mingde.creativeengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 政策问答请求。
 */
public record PolicyAskRequest(
        @NotNull(message = "policyId 不能为空")
        Long policyId,

        @NotBlank(message = "question 不能为空")
        String question) {
}
