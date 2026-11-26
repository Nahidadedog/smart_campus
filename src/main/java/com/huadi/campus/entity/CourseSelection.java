package com.huadi.campus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huadi.campus.enums.SelectionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 选课记录。student_id + course_id 唯一；退选后再选复用同一条记录 */
@Getter
@Setter
@NoArgsConstructor
@TableName("course_selection")
public class CourseSelection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;

    /** 状态由业务层显式赋值，不在实体里给默认值，避免"新对象 == 已选"的误判 */
    private SelectionStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
