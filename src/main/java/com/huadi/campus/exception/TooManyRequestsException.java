package com.huadi.campus.exception;

/** 触发限流（429） */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS, message);
    }
}
