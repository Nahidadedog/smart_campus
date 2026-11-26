package com.huadi.campus.vo;

/** 课程统计：开课总数 + 有效选课记录数 */
public record CourseStatsVO(long totalCourses, long totalSelections) {
}
