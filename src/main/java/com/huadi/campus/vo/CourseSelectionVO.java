package com.huadi.campus.vo;

import com.huadi.campus.entity.CourseSelection;

import java.time.LocalDateTime;

/** 一条选课记录：学生摘要 + 课程详情。学生端和教师端共用 */
public record CourseSelectionVO(
        Long id,
        String status,
        LocalDateTime createTime,
        Long studentId,
        String studentNo,
        String studentName,
        CourseVO course
) {
    public static CourseSelectionVO from(CourseSelection s) {
        var student = s.getStudent();
        return new CourseSelectionVO(
                s.getId(),
                s.getStatus().getLabel(),
                s.getCreateTime(),
                student.getId(),
                student.getStudentNo(),
                student.getUser().getRealName(),
                CourseVO.from(s.getCourse())
        );
    }
}
