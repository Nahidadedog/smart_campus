package com.huadi.campus.selection;

import com.huadi.campus.exception.BusinessException;
import com.huadi.campus.exception.ErrorCode;

/** 落库失败（名额已回补），对用户表现为可重试的服务端错误。 */
public class SelectionPersistException extends BusinessException {

    public SelectionPersistException(Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, "选课处理失败，请稍后重试");
        initCause(cause);
    }
}
