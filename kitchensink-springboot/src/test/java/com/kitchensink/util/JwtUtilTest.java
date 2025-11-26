package com.kitchensink.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private String testSecret = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hmac-sha";
    private Long testExpiration = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
    }

    @Test
    @DisplayName("Should generate token successfully")
    void testGenerateToken() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void testExtractUserId() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        String userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo("user-1");
    }

    @Test
    @DisplayName("Should extract role from token")
    void testExtractRole() {
        String token = jwtUtil.generateToken("user-1", "ADMIN", "test@example.com");
        String role = jwtUtil.extractRole(token);

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should extract expiration from token")
    void testExtractExpiration() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration.after(new Date())).isTrue();
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateToken() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        Boolean isValid = jwtUtil.validateToken(token, "user-1");

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid user ID")
    void testValidateToken_InvalidUserId() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        Boolean isValid = jwtUtil.validateToken(token, "user-2");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for expired token")
    void testValidateToken_ExpiredToken() {
        // Create a token with very short expiration (1ms)
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L);
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        
        // Wait to ensure token expires
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Reset expiration for other tests
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
        
        // Expired tokens throw exception when parsed, so validation fails
        Boolean isValid = false;
        try {
            isValid = jwtUtil.validateToken(token, "user-1");
        } catch (Exception e) {
            // Expected - expired token throws exception during parsing
            isValid = false;
        }
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void testExtractAllClaims() {
        String token = jwtUtil.generateToken("user-1", "USER", "test@example.com");
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        assertThat(userId).isEqualTo("user-1");
        assertThat(role).isEqualTo("USER");
    }
}

