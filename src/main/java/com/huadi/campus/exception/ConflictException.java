package com.huadi.campus.exception;

/** 与当前状态冲突：重复选课、名额已满、用户名已存在等（409） */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
