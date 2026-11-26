package com.huadi.campus.vo;

import java.util.Map;

/** 教师统计：总数 + 按类型 / 院系分布 */
public record TeacherStatsVO(long totalTeachers, Map<String, Long> byType, Map<String, Long> byDepartment) {
}
