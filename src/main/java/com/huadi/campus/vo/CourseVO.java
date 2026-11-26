package com.huadi.campus.vo;

import com.huadi.campus.entity.Course;
import com.huadi.campus.entity.Teacher;

import java.time.LocalDateTime;

/** 课程信息（教师只暴露 id 与姓名） */
public record CourseVO(
        Long id,
        String name,
        String code,
        Double credits,
        Integer hours,
        String type,
        String description,
        Long teacherId,
        String teacherName,
        String semester,
        String schedule,
        String classroom,
        Integer maxStudents,
        Integer currentStudents,
        LocalDateTime createTime
) {
    public static CourseVO from(Course c) {
        Teacher teacher = c.getTeacher();
        String teacherName = teacher != null && teacher.getUser() != null ? teacher.getUser().getRealName() : null;
        Long teacherId = teacher != null ? teacher.getId() : null;
        return new CourseVO(
                c.getId(), c.getName(), c.getCode(), c.getCredits(), c.getHours(), c.getType(), c.getDescription(),
                teacherId, teacherName, c.getSemester(), c.getSchedule(), c.getClassroom(),
                c.getMaxStudents(), c.getCurrentStudents(), c.getCreateTime()
        );
    }
}
