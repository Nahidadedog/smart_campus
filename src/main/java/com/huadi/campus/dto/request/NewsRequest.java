package com.huadi.campus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发布新闻请求（仅管理员） */
public record NewsRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题长度不能超过 200")
        String title,

        @NotBlank(message = "内容不能为空")
        String content,

        @Size(max = 50, message = "分类长度不能超过 50")
        String category,

        @Size(max = 500, message = "封面地址长度不能超过 500")
        String coverImage
) {
}
