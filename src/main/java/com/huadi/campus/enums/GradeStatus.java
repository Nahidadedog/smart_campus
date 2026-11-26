package com.huadi.campus.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 成绩状态：正常 / 补考 */
@Getter
@RequiredArgsConstructor
public enum GradeStatus {

    NORMAL("正常"),
    RETAKE("补考");

    private final String label;
}
