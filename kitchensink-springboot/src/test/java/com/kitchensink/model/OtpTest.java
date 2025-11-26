package com.kitchensink.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Otp Model Tests")
class OtpTest {

    private Otp otp;

    @BeforeEach
    void setUp() {
        otp = new Otp();
    }

    @Test
    @DisplayName("Should create OTP with default values")
    void testOtpCreation() {
        assertThat(otp.isUsed()).isFalse();
        assertThat(otp.getCreatedAt()).isNotNull();
        assertThat(otp.getExpiresAt()).isNotNull();
        assertThat(otp.getExpiresAt()).isAfter(otp.getCreatedAt());
    }

    @Test
    @DisplayName("Should create OTP with constructor")
    void testOtpConstructor() {
        Otp newOtp = new Otp("test@example.com", "123456", "LOGIN");

        assertThat(newOtp.getEmail()).isEqualTo("test@example.com");
        assertThat(newOtp.getOtpCode()).isEqualTo("123456");
        assertThat(newOtp.getPurpose()).isEqualTo("LOGIN");
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        otp.setId("otp-1");
        otp.setEmail("test@example.com");
        otp.setEmailHash("email-hash");
        otp.setOtpCode("123456");
        otp.setOtpHash("otp-hash");
        LocalDateTime now = LocalDateTime.now();
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusMinutes(10));
        otp.setUsed(true);
        otp.setPurpose("LOGIN");

        assertThat(otp.getId()).isEqualTo("otp-1");
        assertThat(otp.getEmail()).isEqualTo("test@example.com");
        assertThat(otp.getEmailHash()).isEqualTo("email-hash");
        assertThat(otp.getOtpCode()).isEqualTo("123456");
        assertThat(otp.getOtpHash()).isEqualTo("otp-hash");
        assertThat(otp.getCreatedAt()).isEqualTo(now);
        assertThat(otp.getExpiresAt()).isEqualTo(now.plusMinutes(10));
        assertThat(otp.isUsed()).isTrue();
        assertThat(otp.getPurpose()).isEqualTo("LOGIN");
    }

    @Test
    @DisplayName("Should check if OTP is expired")
    void testIsExpired() {
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThat(otp.isExpired()).isTrue();

        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        assertThat(otp.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should check if OTP is valid")
    void testIsValid() {
        otp.setUsed(false);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        assertThat(otp.isValid()).isTrue();

        otp.setUsed(true);
        assertThat(otp.isValid()).isFalse();

        otp.setUsed(false);
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThat(otp.isValid()).isFalse();
    }
}

