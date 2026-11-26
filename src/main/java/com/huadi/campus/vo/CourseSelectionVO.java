package com.huadi.campus.vo;

import com.huadi.campus.enums.SelectionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 一条选课记录：学生摘要 + 课程详情。学生端和教师端共用 */
@Data
public class CourseSelectionVO {
    private Long id;
    private SelectionStatus status;
    private LocalDateTime createTime;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private CourseVO course;
}
