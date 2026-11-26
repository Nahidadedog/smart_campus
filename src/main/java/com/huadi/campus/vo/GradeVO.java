package com.huadi.campus.vo;

import com.huadi.campus.enums.GradeLevel;
import com.huadi.campus.enums.GradeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 成绩（等级 / 状态为枚举，序列化时输出中文标签） */
@Data
public class GradeVO {
    private Long id;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long courseId;
    private String courseName;
    private Double score;
    private GradeLevel gradeLevel;
    private Double credits;
    private GradeStatus status;
    private String semester;
    private LocalDateTime createTime;
}
