package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.dto.request.LoginRequest;
import com.huadi.campus.dto.request.RegisterRequest;
import com.huadi.campus.service.AuthService;
import com.huadi.campus.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录 / 学生自助注册")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "返回 JWT 与用户基本信息，后续请求携带 Authorization: Bearer <token>")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "学生注册", description = "仅限学生角色，注册成功后直接返回 token")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }
}
