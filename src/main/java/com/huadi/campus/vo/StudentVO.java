package com.huadi.campus.vo;

import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.User;

import java.time.LocalDateTime;

/** 学生信息（已把关联的 User 字段拍平，不含密码） */
public record StudentVO(
        Long id,
        Long userId,
        String username,
        String realName,
        String phone,
        String email,
        String studentNo,
        String department,
        String major,
        String className,
        String enrollmentYear,
        String status,
        String dormitory,
        Double totalCredits,
        Double cardBalance,
        LocalDateTime createTime
) {
    public static StudentVO from(Student s) {
        User u = s.getUser();
        return new StudentVO(
                s.getId(), u.getId(), u.getUsername(), u.getRealName(), u.getPhone(), u.getEmail(),
                s.getStudentNo(), s.getDepartment(), s.getMajor(), s.getClassName(), s.getEnrollmentYear(),
                s.getStatus(), s.getDormitory(), s.getTotalCredits(), s.getCardBalance(), s.getCreateTime()
        );
    }
}
