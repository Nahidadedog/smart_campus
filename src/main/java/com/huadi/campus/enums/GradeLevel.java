package com.huadi.campus.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 成绩等级（按百分制分数映射） */
@Getter
@RequiredArgsConstructor
public enum GradeLevel {

    EXCELLENT("优"),
    GOOD("良"),
    MEDIUM("中"),
    PASS("及格"),
    FAIL("不及格");

    private final String label;

    public static GradeLevel fromScore(double score) {
        if (score >= 90) return EXCELLENT;
        if (score >= 80) return GOOD;
        if (score >= 70) return MEDIUM;
        if (score >= 60) return PASS;
        return FAIL;
    }
}
