package com.huadi.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过 50")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
