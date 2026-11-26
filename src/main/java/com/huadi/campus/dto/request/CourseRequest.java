package com.huadi.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 管理员创建 / 修改课程 */
public record CourseRequest(
        @NotBlank(message = "课程名称不能为空")
        @Size(max = 100) String name,

        @NotBlank(message = "课程代码不能为空")
        @Size(max = 20) String code,

        Double credits,
        Integer hours,
        String type,
        @Size(max = 200) String description,
        Long teacherId,
        String semester,
        @Size(max = 100) String schedule,
        @Size(max = 100) String classroom,

        @NotNull(message = "名额上限不能为空")
        @PositiveOrZero(message = "名额上限不能为负数")
        Integer maxStudents,

        /** 选课时间窗口，null 表示不限制 */
        LocalDateTime enrollStartTime,
        LocalDateTime enrollEndTime
) {
}
