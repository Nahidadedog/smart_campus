package com.huadi.campus.vo;

import java.util.Map;

/** 学生统计：总数 + 按院系 / 状态分布 */
public record StudentStatsVO(long totalStudents, Map<String, Long> byDepartment, Map<String, Long> byStatus) {
}
