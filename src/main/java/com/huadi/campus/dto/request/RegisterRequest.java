package com.huadi.campus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 学生自助注册请求。
 * 教师 / 领导 / 管理员不开放注册，由管理员在后台创建，避免提权。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 20, message = "用户名长度需在 4-20 个字符之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度需在 6-50 个字符之间")
        String password,

        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 100, message = "姓名长度不能超过 100")
        String realName,

        @NotBlank(message = "学号不能为空")
        @Size(max = 20, message = "学号长度不能超过 20")
        String studentNo,

        @NotBlank(message = "院系不能为空")
        String department,

        @Size(max = 100)
        String major,

        @Size(max = 50)
        String className,

        @NotBlank(message = "入学年份不能为空")
        @Pattern(regexp = "^\\d{4}$", message = "入学年份格式应为 4 位数字")
        String enrollmentYear,

        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @Email(message = "邮箱格式不正确")
        String email
) {
}
