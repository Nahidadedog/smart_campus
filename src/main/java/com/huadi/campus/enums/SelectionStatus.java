package com.huadi.campus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

/**
 * 选课记录状态：已选 / 退选。退选后同一门课再次选择会复用原记录。
 * 数据库存枚举名（ENROLLED / DROPPED），接口输出中文标签（@JsonValue）。
 */
@RequiredArgsConstructor
public enum SelectionStatus {

    ENROLLED("已选"),
    DROPPED("退选");

    private final String label;

    @JsonValue
    public String getLabel() {
        return label;
    }
}
