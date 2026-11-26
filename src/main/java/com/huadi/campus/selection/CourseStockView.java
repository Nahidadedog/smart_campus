package com.huadi.campus.selection;

import com.huadi.campus.entity.Course;

import java.time.LocalDateTime;

/**
 * 课程名额快照：选课热路径上用本地缓存判断"课程是否存在、是否在选课时间内"，
 * 不用每次请求都查数据库。真正扣名额仍然走 StockService，所以快照短暂过期不会导致超选。
 */
public record CourseStockView(Long id, Integer maxStudents,
                              LocalDateTime enrollStartTime,
                              LocalDateTime enrollEndTime) {

    private static final CourseStockView MISSING = new CourseStockView(null, 0, null, null);

    public static CourseStockView missing() {
        return MISSING;
    }

    public static CourseStockView of(Course course) {
        if (course == null) {
            return MISSING;
        }
        return new CourseStockView(course.getId(),
                course.getMaxStudents() == null ? 0 : course.getMaxStudents(),
                course.getEnrollStartTime(), course.getEnrollEndTime());
    }

    public boolean isMissing() {
        return id == null;
    }

    /** 开始 / 结束时间为空表示不限制 */
    public boolean isOpenNow() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = enrollStartTime == null || !now.isBefore(enrollStartTime);
        boolean beforeEnd = enrollEndTime == null || now.isBefore(enrollEndTime);
        return afterStart && beforeEnd;
    }
}
