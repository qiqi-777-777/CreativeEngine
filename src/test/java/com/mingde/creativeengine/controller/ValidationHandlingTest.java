package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.dto.LoginRequest;
import com.mingde.creativeengine.dto.RegisterRequest;
import com.mingde.creativeengine.dto.UpdateUserProfileRequest;
import com.mingde.creativeengine.exception.GlobalExceptionHandler;

import jakarta.validation.Valid;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 参数校验测试：确认 @Valid 在 MVC 层真正生效，且校验失败时返回的是可读的字段错误信息。
 *
 * <p>用 standaloneSetup + 一个只做透传的桩 Controller，不依赖数据库、拦截器与真实业务。</p>
 */
class ValidationHandlingTest {

    private final MockMvc mockMvc;

    ValidationHandlingTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void rejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/probe/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"  \",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    void rejectsTooShortPasswordOnRegister() throws Exception {
        mockMvc.perform(post("/probe/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"123\"}"))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("密码长度不能少于6位"));
    }

    @Test
    void rejectsIllegalPhone() throws Exception {
        mockMvc.perform(put("/probe/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"小明\",\"phone\":\"12345\"}"))
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));
    }

    @Test
    void allowsEmptyOptionalContactFields() throws Exception {
        mockMvc.perform(put("/probe/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"小明\",\"phone\":\"\",\"email\":\"\"}"))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 请求体不是合法 JSON 属于调用方写错，要按参数错误回，不能落到 5000「系统错误」。
     */
    @Test
    void malformedJsonIsReportedAsParamError() throws Exception {
        mockMvc.perform(post("/probe/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\" \"password\":\"secret123\"}"))
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_INVALID.getCode()));
    }

    /**
     * 仅用于触发的桩 Controller：把请求体绑到 DTO 上，方法体不关心内容。
     */
    @RestController
    @RequestMapping("/probe")
    static class ValidationProbeController {

        @PostMapping("/login")
        public Result<Void> login(@Valid @RequestBody LoginRequest request) {
            return Result.success();
        }

        @PostMapping("/register")
        public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
            return Result.success();
        }

        @PutMapping("/profile")
        public Result<Void> profile(@Valid @RequestBody UpdateUserProfileRequest request) {
            return Result.success();
        }
    }
}
