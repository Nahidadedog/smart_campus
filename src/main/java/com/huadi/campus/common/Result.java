package com.huadi.campus.common;

/**
 * 统一响应体：{ code, message, data }
 * <p>
 * 成功时 code 恒为 200；失败时 code 与 HTTP 状态码保持一致，方便前端统一处理。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
