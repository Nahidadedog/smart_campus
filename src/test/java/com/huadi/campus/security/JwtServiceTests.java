package com.huadi.campus.security;

import com.huadi.campus.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTests {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("unit-test-secret-key-unit-test-secret-key-0123456789", Duration.ofMinutes(5)));

    @Test
    void generateThenParseRoundTripsClaims() {
        String token = jwtService.generateToken(42L, "student1", UserRole.STUDENT);

        JwtService.TokenPayload payload = jwtService.parseToken(token);

        assertEquals(42L, payload.userId());
        assertEquals("student1", payload.username());
        assertEquals(UserRole.STUDENT, payload.role());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(1L, "admin", UserRole.ADMIN);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtService.parseToken(tampered));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(new JwtProperties("too-short", Duration.ofMinutes(5))));
    }
}
