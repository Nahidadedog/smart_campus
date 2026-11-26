package com.huadi.campus.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 教师录入 / 更新成绩请求（同一学生同一课程同一学期 upsert） */
public record GradeRequest(
        @NotNull(message = "学生不能为空")
        Long studentId,

        @NotNull(message = "分数不能为空")
        @DecimalMin(value = "0", message = "分数不能小于 0")
        @DecimalMax(value = "100", message = "分数不能大于 100")
        Double score,

        @NotBlank(message = "学期不能为空")
        @Pattern(regexp = "^\\d{4}-\\d{4}-[12]$", message = "学期格式应为 2025-2026-1")
        String semester
) {
}
