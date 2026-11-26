package com.huadi.campus.security;

import com.huadi.campus.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** JWT 签发与解析。签名密钥在启动时计算一次并复用。 */
@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        String secret = properties.secret();
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 未配置或长度不足 32 字节，请通过环境变量 JWT_SECRET 设置");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
    }

    public String generateToken(Long userId, String username, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析并校验 token（签名 + 过期时间）。
     *
     * @throws JwtException token 无效或已过期
     */
    public TokenPayload parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        Claims claims = jws.getPayload();
        return new TokenPayload(
                claims.get(CLAIM_USER_ID, Long.class),
                claims.getSubject(),
                UserRole.valueOf(claims.get(CLAIM_ROLE, String.class))
        );
    }

    public Duration getExpiration() {
        return expiration;
    }

    /** 从 JWT 中解析出的用户信息，也是放入 SecurityContext 的凭证载荷 */
    public record TokenPayload(Long userId, String username, UserRole role) {
    }
}
