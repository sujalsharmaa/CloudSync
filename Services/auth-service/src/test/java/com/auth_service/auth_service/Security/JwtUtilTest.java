// Services/auth-service/src/test/java/com/auth_service/auth_service/Security/JwtUtilTest.java
package com.auth_service.auth_service.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "mySecretKey123456789012345678901234567890adsf54sdf54s45fs54df54";
    private final long jwtExpirationMs = 86400000; // 24 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", jwtExpirationMs);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // Arrange
        String email = "test@example.com";
        Long userId = 1L;
        String name = "Test User";

        // Act
        String token = jwtUtil.generateToken(email, userId, name);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getEmailFromToken_ShouldReturnCorrectEmail() {
        // Arrange
        String email = "test@example.com";
        Long userId = 1L;
        String name = "Test User";
        String token = jwtUtil.generateToken(email, userId, name);

        // Act
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // Assert
        assertEquals(email, extractedEmail);
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectUserId() {
        // Arrange
        String email = "test@example.com";
        Long userId = 123L;
        String name = "Test User";
        String token = jwtUtil.generateToken(email, userId, name);

        // Act
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);

        // Assert
        assertEquals(userId, extractedUserId);
    }

    @Test
    void validateToken_ValidToken_ShouldReturnTrue() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com", 1L, "Test");

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_NewToken_ShouldReturnFalse() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com", 1L, "Test");

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }
}