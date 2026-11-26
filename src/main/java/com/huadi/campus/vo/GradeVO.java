package com.huadi.campus.vo;

import com.huadi.campus.entity.Grade;

import java.time.LocalDateTime;

/** 成绩（等级 / 状态直接给出中文标签，前端无需再做映射） */
public record GradeVO(
        Long id,
        Long studentId,
        String studentNo,
        String studentName,
        Long courseId,
        String courseName,
        Double score,
        String gradeLevel,
        Double credits,
        String status,
        String semester,
        LocalDateTime createTime
) {
    public static GradeVO from(Grade g) {
        var student = g.getStudent();
        var user = student.getUser();
        return new GradeVO(
                g.getId(), student.getId(), student.getStudentNo(), user.getRealName(),
                g.getCourse().getId(), g.getCourse().getName(), g.getScore(),
                g.getGradeLevel().getLabel(), g.getCredits(), g.getStatus().getLabel(),
                g.getSemester(), g.getCreateTime()
        );
    }
}
