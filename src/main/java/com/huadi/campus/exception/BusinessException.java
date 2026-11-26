package com.huadi.campus.exception;

import lombok.Getter;

/** 业务异常基类。Service 层只抛业务异常，由 GlobalExceptionHandler 统一转换为响应。 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
