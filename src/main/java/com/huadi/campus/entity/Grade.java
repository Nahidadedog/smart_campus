package com.huadi.campus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huadi.campus.enums.GradeLevel;
import com.huadi.campus.enums.GradeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 成绩。student_id + course_id + semester 唯一，录成绩时按此 upsert */
@Getter
@Setter
@NoArgsConstructor
@TableName("grade")
public class Grade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;

    private Double score;
    private GradeLevel gradeLevel;
    private Double credits;

    /** 状态由业务层根据分数计算，不在实体里给默认值 */
    private GradeStatus status;

    private String semester;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
