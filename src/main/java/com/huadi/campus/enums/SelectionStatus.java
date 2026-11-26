package com.huadi.campus.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 选课记录状态：已选 / 退选。退选后同一门课再次选择会复用原记录。 */
@Getter
@RequiredArgsConstructor
public enum SelectionStatus {

    ENROLLED("已选"),
    DROPPED("退选");

    private final String label;
}
