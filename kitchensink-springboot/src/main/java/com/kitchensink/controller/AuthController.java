package com.kitchensink.controller;

import com.kitchensink.dto.LoginRequestDTO;
import com.kitchensink.dto.LoginResponseDTO;
import com.kitchensink.dto.OtpResponseDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.service.AuthenticationService;
import com.kitchensink.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationService authenticationService;
    
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    @PostMapping("/login/request-otp")
    @Operation(summary = "Request login OTP", description = "Request OTP to be sent to email for login")
    public ResponseEntity<Response<OtpResponseDTO>> requestLoginOtp(@Valid @RequestBody LoginRequestDTO request) {
        logger.debug("Login OTP requested for email: [REDACTED]");
        
        authenticationService.requestLoginOtp(request.getEmail());
        
        OtpResponseDTO responseData = new OtpResponseDTO("OTP sent to email");
        Response<OtpResponseDTO> response = Response.success(responseData, "OTP sent successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login/verify")
    @Operation(summary = "Verify OTP and login", description = "Verify OTP and get JWT token")
    public ResponseEntity<Response<LoginResponseDTO>> verifyOtpAndLogin(@Valid @RequestBody LoginRequestDTO request) {
        logger.debug("OTP verification requested for email: [REDACTED]");
        
        if (request.getOtp() == null || request.getOtp().isEmpty()) {
            Response<LoginResponseDTO> errorResponse = Response.error("OTP is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        LoginResponseDTO loginResponse = authenticationService.verifyOtpAndLogin(request.getEmail(), request.getOtp());
        
        Response<LoginResponseDTO> response = Response.success(loginResponse, "Login successful");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
}

