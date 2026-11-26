package com.huadi.campus.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 教师信息（已把关联的 User 字段拍平，不含密码） */
@Data
public class TeacherVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String teacherNo;
    private String department;
    private String title;
    private String researchArea;
    private String teacherType;
    private String office;
    private String politicalStatus;
    private LocalDateTime createTime;
}
