package com.huadi.campus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/** 修改联系方式：两个字段都可选，传哪个改哪个 */
public record ContactUpdateRequest(
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @Email(message = "邮箱格式不正确")
        String email
) {
}
