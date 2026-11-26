package com.huadi.campus.exception;

/** 未登录 / 凭证无效（401） */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
