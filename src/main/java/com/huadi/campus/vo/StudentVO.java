package com.huadi.campus.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生信息（已把关联的 User 字段拍平，不含密码） */
@Data
public class StudentVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String studentNo;
    private String department;
    private String major;
    private String className;
    private String enrollmentYear;
    private String status;
    private String dormitory;
    private Double totalCredits;
    private Double cardBalance;
    private LocalDateTime createTime;
}
