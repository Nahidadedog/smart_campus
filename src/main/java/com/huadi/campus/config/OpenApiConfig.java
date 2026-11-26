package com.huadi.campus.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 文档元信息，并在 Swagger UI 中启用 "Authorize" 按钮（Bearer JWT） */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "智慧校园服务平台 API", version = "v2", description = "学生 / 教师 / 领导 / 管理员四类角色的校园服务接口"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
