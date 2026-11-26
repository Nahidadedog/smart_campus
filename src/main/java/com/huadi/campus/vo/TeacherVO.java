package com.huadi.campus.vo;

import com.huadi.campus.entity.Teacher;

import java.time.LocalDateTime;

/** 教师信息（已把关联的 User 字段拍平，不含密码） */
public record TeacherVO(
        Long id,
        Long userId,
        String username,
        String realName,
        String phone,
        String email,
        String teacherNo,
        String department,
        String title,
        String researchArea,
        String teacherType,
        String office,
        String politicalStatus,
        LocalDateTime createTime
) {
    public static TeacherVO from(Teacher t) {
        var u = t.getUser();
        return new TeacherVO(
                t.getId(), u.getId(), u.getUsername(), u.getRealName(), u.getPhone(), u.getEmail(),
                t.getTeacherNo(), t.getDepartment(), t.getTitle(), t.getResearchArea(),
                t.getTeacherType(), t.getOffice(), t.getPoliticalStatus(), t.getCreateTime()
        );
    }
}
