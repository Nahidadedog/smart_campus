package com.huadi.campus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

/** 成绩状态：正常 / 补考。数据库存枚举名，接口输出中文标签 */
@RequiredArgsConstructor
public enum GradeStatus {

    NORMAL("正常"),
    RETAKE("补考");

    private final String label;

    @JsonValue
    public String getLabel() {
        return label;
    }
}
