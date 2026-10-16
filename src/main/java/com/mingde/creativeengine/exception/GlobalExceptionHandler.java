package com.mingde.creativeengine.exception;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器
 *
 * @author CreativeEngine Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（{@code @Valid} 校验失败）。
     *
     * <p>把第一个字段错误信息回给调用方，而不是笼统的「参数错误」——
     * 前端可以直接把这句话弹给用户。</p>
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 大模型调用被限流（RequestNotPermitted）或熔断（CallNotPermitted）。
     *
     * <p>这不是系统故障，而是保护机制按预期生效，所以给一个可读提示 + 独立错误码，
     * 前端可以据此提示"稍后重试"，也便于和真正的 5000 系统异常区分开。</p>
     */
    @ExceptionHandler({CallNotPermittedException.class, RequestNotPermitted.class})
    public Result<Void> handleModelCallRejected(Exception e) {
        log.warn("大模型调用被限流或熔断：{}", e.getMessage());
        return Result.error(ResultCode.AI_SERVICE_BUSY);
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }

}

