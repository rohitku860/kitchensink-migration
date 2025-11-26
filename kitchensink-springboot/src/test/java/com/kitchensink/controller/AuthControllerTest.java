package com.kitchensink.controller;

import com.kitchensink.dto.LoginRequestDTO;
import com.kitchensink.dto.LoginResponseDTO;
import com.kitchensink.dto.OtpResponseDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthController authController;

    private LoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp() {
        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should request login OTP successfully")
    void testRequestLoginOtp() {
        doNothing().when(authenticationService).requestLoginOtp(anyString());

        ResponseEntity<Response<OtpResponseDTO>> response = authController.requestLoginOtp(loginRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getMessage()).isEqualTo("OTP sent to email");
        verify(authenticationService).requestLoginOtp("test@example.com");
    }

    @Test
    @DisplayName("Should verify OTP and login successfully")
    void testVerifyOtpAndLogin() {
        loginRequestDTO.setOtp("123456");
        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setToken("jwt-token");
        loginResponse.setUserId("user-1");
        loginResponse.setEmail("test@example.com");
        loginResponse.setRole("USER");

        when(authenticationService.verifyOtpAndLogin("test@example.com", "123456")).thenReturn(loginResponse);

        ResponseEntity<Response<LoginResponseDTO>> response = authController.verifyOtpAndLogin(loginRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token");
        verify(authenticationService).verifyOtpAndLogin("test@example.com", "123456");
    }

    @Test
    @DisplayName("Should return bad request when OTP is missing")
    void testVerifyOtpAndLogin_MissingOtp() {
        loginRequestDTO.setOtp(null);

        ResponseEntity<Response<LoginResponseDTO>> response = authController.verifyOtpAndLogin(loginRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getMessage()).isEqualTo("OTP is required");
        verify(authenticationService, never()).verifyOtpAndLogin(anyString(), anyString());
    }

    @Test
    @DisplayName("Should return bad request when OTP is empty")
    void testVerifyOtpAndLogin_EmptyOtp() {
        loginRequestDTO.setOtp("");

        ResponseEntity<Response<LoginResponseDTO>> response = authController.verifyOtpAndLogin(loginRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        verify(authenticationService, never()).verifyOtpAndLogin(anyString(), anyString());
    }
}

