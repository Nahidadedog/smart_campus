package com.huadi.campus.exception;

import com.huadi.campus.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把所有异常统一转换为 { code, message, data } 响应。
 * code 与 HTTP 状态码一致，前端只需根据 code 判断。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：使用其携带的 HTTP 状态码 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        return build(e.getErrorCode().getHttpStatus(), e.getMessage());
    }

    /** @Valid 校验失败（@RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("参数校验失败");
        return build(HttpStatus.BAD_REQUEST.value(), message);
    }

    /** @Validated 校验失败（@RequestParam / @PathVariable） */
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Result<Void>> handleConstraintViolation(Exception e) {
        return build(HttpStatus.BAD_REQUEST.value(), "参数校验失败");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleUnreadableMessage(Exception e) {
        return build(HttpStatus.BAD_REQUEST.value(), "请求参数格式不正确");
    }

    /** 方法级安全校验（@PreAuthorize）抛出 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN.value(), "没有权限执行此操作");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException e) {
        return build(HttpStatus.UNAUTHORIZED.value(), "登录已失效，请重新登录");
    }

    /** 唯一键冲突等数据库约束错误 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT.value(), "数据冲突，请检查是否重复提交");
    }

    /** 访问了不存在的路径 / 静态资源 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND.value(), "资源不存在");
    }

    /** 兜底：只返回通用提示，细节写日志，避免泄露内部信息 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未处理的异常", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器繁忙，请稍后重试");
    }

    private ResponseEntity<Result<Void>> build(int status, String message) {
        return ResponseEntity.status(status).body(Result.error(status, message));
    }
}
