package com.kitchensink.service;

import com.kitchensink.exception.ResourceConflictException;
import com.kitchensink.model.Otp;
import com.kitchensink.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Tests")
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private OtpService otpService;

    private Otp testOtp;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "maxAttemptsPerWindow", 1000);
        ReflectionTestUtils.setField(otpService, "rateLimitWindowMinutes", 15);

        testOtp = new Otp();
        testOtp.setId("otp-1");
        testOtp.setEmail("test@example.com");
        testOtp.setOtpCode("123456");
        testOtp.setOtpHash("otp-hash");
        testOtp.setEmailHash("email-hash");
        testOtp.setPurpose("LOGIN");
        testOtp.setUsed(false);
        testOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    @Test
    @DisplayName("Should generate OTP successfully")
    void testGenerateOtp() {
        String otp = otpService.generateOtp();

        assertThat(otp).isNotNull();
        assertThat(otp.length()).isEqualTo(6);
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    @DisplayName("Should create OTP successfully")
    void testCreateOtp_Success() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(otpRepository.countByEmailHashAndPurposeAndCreatedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(encryptionService.hash(anyString())).thenReturn("otp-hash");
        when(otpRepository.save(any(Otp.class))).thenReturn(testOtp);

        Otp result = otpService.createOtp("test@example.com", "LOGIN");

        assertThat(result).isNotNull();
        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    @DisplayName("Should throw exception when rate limit exceeded")
    void testCreateOtp_RateLimitExceeded() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(otpRepository.countByEmailHashAndPurposeAndCreatedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(1001L);

        assertThatThrownBy(() -> otpService.createOtp("test@example.com", "LOGIN"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Maximum OTP attempts");
    }

    @Test
    @DisplayName("Should invalidate existing unused OTPs")
    void testCreateOtp_InvalidateExisting() {
        Otp existingOtp = new Otp();
        existingOtp.setId("otp-old");
        existingOtp.setUsed(false);

        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(otpRepository.countByEmailHashAndPurposeAndCreatedAtAfter(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse(anyString(), anyString()))
                .thenReturn(Collections.singletonList(existingOtp));
        when(otpRepository.save(any(Otp.class))).thenReturn(testOtp);
        lenient().when(encryptionService.hash(anyString())).thenReturn("otp-hash");

        otpService.createOtp("test@example.com", "LOGIN");

        verify(otpRepository, atLeast(2)).save(any(Otp.class));
    }

    @Test
    @DisplayName("Should verify OTP successfully")
    void testVerifyOtp_Success() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("123456")).thenReturn("otp-hash");
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse("email-hash", "LOGIN"))
                .thenReturn(Collections.singletonList(testOtp));
        when(otpRepository.save(any(Otp.class))).thenReturn(testOtp);

        boolean result = otpService.verifyOtp("test@example.com", "123456", "LOGIN");

        assertThat(result).isTrue();
        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    @DisplayName("Should return false when OTP not found")
    void testVerifyOtp_NotFound() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("123456")).thenReturn("otp-hash");
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse("email-hash", "LOGIN"))
                .thenReturn(Collections.emptyList());

        boolean result = otpService.verifyOtp("test@example.com", "123456", "LOGIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when OTP hash doesn't match")
    void testVerifyOtp_InvalidHash() {
        Otp wrongOtp = new Otp();
        wrongOtp.setOtpHash("wrong-hash");

        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("123456")).thenReturn("otp-hash");
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse("email-hash", "LOGIN"))
                .thenReturn(Collections.singletonList(wrongOtp));

        boolean result = otpService.verifyOtp("test@example.com", "123456", "LOGIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when OTP is expired")
    void testVerifyOtp_Expired() {
        testOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("123456")).thenReturn("otp-hash");
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse("email-hash", "LOGIN"))
                .thenReturn(Collections.singletonList(testOtp));

        boolean result = otpService.verifyOtp("test@example.com", "123456", "LOGIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get OTP by email and code successfully")
    void testGetOtpByEmailAndCode_Success() {
        when(encryptionService.hash("test@example.com")).thenReturn("email-hash");
        when(encryptionService.hash("123456")).thenReturn("otp-hash");
        when(otpRepository.findByEmailHashAndPurposeAndUsedFalse("email-hash", "EMAIL_CHANGE"))
                .thenReturn(Collections.singletonList(testOtp));

        Optional<Otp> result = otpService.getOtpByEmailAndCode("test@example.com", "123456");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("otp-1");
    }

    @Test
    @DisplayName("Should cleanup expired OTPs")
    void testCleanupExpiredOtps() {
        doNothing().when(otpRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));

        otpService.cleanupExpiredOtps();

        verify(otpRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}

