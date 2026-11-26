package com.huadi.campus.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * jwt.secret / jwt.expiration
 * <p>
 * secret 要求至少 32 字节（HS256 规范），生产环境通过环境变量 JWT_SECRET 注入。
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, @DefaultValue("24h") Duration expiration) {
}
