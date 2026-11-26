package com.huadi.campus.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 业务错误码：同时决定 HTTP 状态码与响应体中的 code */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    INTERNAL_ERROR(500);

    private final int httpStatus;
}
