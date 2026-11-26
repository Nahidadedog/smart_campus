package com.huadi.campus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private Double credits;
    private Integer hours;
    private String type;
    private String description;

    /** 授课教师 teacher.id，可为空 */
    private Long teacherId;

    private String semester;
    private String schedule;
    private String classroom;
    private Integer maxStudents;

    /** 选课时间窗口，null 表示不限制。只用于请求前置校验，不参与名额扣减 */
    private LocalDateTime enrollStartTime;
    private LocalDateTime enrollEndTime;

    /** 已选人数。修改必须通过 CourseMapper 的原子更新语句，避免并发超选 */
    private Integer currentStudents = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
