package com.huadi.campus.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 课程信息（教师只暴露 id 与姓名）。既是课程列表的元素，也内嵌在选课记录里 */
@Data
public class CourseVO {
    private Long id;
    private String name;
    private String code;
    private Double credits;
    private Integer hours;
    private String type;
    private String description;
    private Long teacherId;
    private String teacherName;
    private String semester;
    private String schedule;
    private String classroom;
    private Integer maxStudents;
    private Integer currentStudents;
    private LocalDateTime enrollStartTime;
    private LocalDateTime enrollEndTime;
    private LocalDateTime createTime;
}
