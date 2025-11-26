package com.kitchensink.service;

import com.kitchensink.dto.LoginResponseDTO;
import com.kitchensink.exception.ResourceNotFoundException;
import com.kitchensink.model.Otp;
import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.model.UserRole;
import com.kitchensink.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private Otp testOtp;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setStatus("ACTIVE");

        testOtp = new Otp();
        testOtp.setId("otp-1");
        testOtp.setEmail("test@example.com");
        testOtp.setOtpCode("123456");
        testOtp.setPurpose("LOGIN");
        testOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    @Test
    @DisplayName("Should request login OTP successfully")
    void testRequestLoginOtp_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(otpService.createOtp("test@example.com", "LOGIN")).thenReturn(testOtp);
        doNothing().when(emailService).sendLoginOtp(anyString(), anyString());

        authenticationService.requestLoginOtp("test@example.com");

        verify(userService).getUserByEmail("test@example.com");
        verify(otpService).createOtp("test@example.com", "LOGIN");
        verify(emailService).sendLoginOtp("test@example.com", "123456");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testRequestLoginOtp_UserNotFound() {
        when(userService.getUserByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.requestLoginOtp("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userService).getUserByEmail("nonexistent@example.com");
        verify(otpService, never()).createOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("Should verify OTP and login successfully")
    void testVerifyOtpAndLogin_Success() {
        when(otpService.verifyOtp("test@example.com", "123456", "LOGIN")).thenReturn(true);
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(userService).updateLastLoginDate("user-1");
        when(roleService.getRoleNameByUserId("user-1")).thenReturn("USER");
        when(roleService.getRoleIdByUserId("user-1")).thenReturn("role-1");
        when(jwtUtil.generateToken("user-1", "USER", "test@example.com")).thenReturn("jwt-token");

        LoginResponseDTO response = authenticationService.verifyOtpAndLogin("test@example.com", "123456");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");
        verify(userService).updateLastLoginDate("user-1");
    }

    @Test
    @DisplayName("Should throw exception when OTP is invalid")
    void testVerifyOtpAndLogin_InvalidOtp() {
        when(otpService.verifyOtp("test@example.com", "wrong-otp", "LOGIN")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.verifyOtpAndLogin("test@example.com", "wrong-otp"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(otpService).verifyOtp("test@example.com", "wrong-otp", "LOGIN");
        verify(userService, never()).updateLastLoginDate(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found during verification")
    void testVerifyOtpAndLogin_UserNotFound() {
        when(otpService.verifyOtp("test@example.com", "123456", "LOGIN")).thenReturn(true);
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.verifyOtpAndLogin("test@example.com", "123456"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

