package com.kitchensink.service;

import com.kitchensink.model.User;
import com.kitchensink.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    
    public AuthenticationService(UserService userService, OtpService otpService,
                                 EmailService emailService, JwtUtil jwtUtil, RoleService roleService) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.roleService = roleService;
    }
    
    /**
     * Request OTP for login
     */
    public void requestLoginOtp(String email) {
        logger.debug("Requesting login OTP for email: [REDACTED]");
        
        // Check if user exists
        userService.getUserByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Login attempt with non-existent email: [REDACTED]");
                    return new com.kitchensink.exception.ResourceNotFoundException("User", email);
                });
        
        // Generate and save OTP
        com.kitchensink.model.Otp otp = otpService.createOtp(email, "LOGIN");
        
        // Send OTP via email
        emailService.sendLoginOtp(email, otp.getOtpCode());
        
        logger.info("Login OTP sent successfully");
    }
    
    /**
     * Verify OTP and generate JWT token
     */
    public Map<String, Object> verifyOtpAndLogin(String email, String otpCode) {
        logger.debug("Verifying OTP for login: [REDACTED]");
        
        // Verify OTP
        boolean isValid = otpService.verifyOtp(email, otpCode, "LOGIN");
        if (!isValid) {
            logger.warn("Invalid OTP for email: [REDACTED]");
            throw new com.kitchensink.exception.ResourceNotFoundException("Invalid OTP", "otp");
        }
        
        // Get user
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("User", email));
        
        // Update last login date
        userService.updateLastLoginDate(user.getId());
        
        // Get role name from user_roles collection
        String roleName = roleService.getRoleNameByUserId(user.getId());
        String roleId = roleService.getRoleIdByUserId(user.getId());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), roleName, user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("role", roleName);
        response.put("roleId", roleId);
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        
        logger.info("User logged in successfully: {}", user.getId());
        return response;
    }
}

