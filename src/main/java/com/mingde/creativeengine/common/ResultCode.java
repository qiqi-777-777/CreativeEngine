package com.mingde.creativeengine.common;

/**
 * 响应状态码枚举
 *
 * @author CreativeEngine Team
 */
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    ERROR(1, "操作失败"),
    
    // 参数相关 1xxx
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    PARAM_INVALID(1003, "参数格式不正确"),
    
    // 用户相关 2xxx
    USER_NOT_EXIST(2001, "用户不存在"),
    USER_NOT_LOGIN(2002, "用户未登录"),
    TOKEN_INVALID(2003, "Token无效"),
    TOKEN_EXPIRED(2004, "Token已过期"),
    PERMISSION_DENIED(2005, "权限不足"),
    
    // 业务相关 3xxx
    DATA_NOT_EXIST(3001, "数据不存在"),
    DATA_EXISTED(3002, "数据已存在"),
    OPERATION_FAILED(3003, "操作失败"),
    STATUS_ERROR(3004, "状态错误"),
    
    // 文件相关 4xxx
    FILE_UPLOAD_ERROR(4001, "文件上传失败"),
    FILE_TYPE_ERROR(4002, "文件类型不支持"),
    FILE_SIZE_ERROR(4003, "文件大小超出限制"),
    
    // 系统相关 5xxx
    SYSTEM_ERROR(5000, "系统错误"),
    DATABASE_ERROR(5001, "数据库错误"),
    NETWORK_ERROR(5002, "网络错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}

