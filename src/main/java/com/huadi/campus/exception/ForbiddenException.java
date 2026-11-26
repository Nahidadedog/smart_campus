package com.huadi.campus.exception;

/** 已登录但权限不足（403） */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
