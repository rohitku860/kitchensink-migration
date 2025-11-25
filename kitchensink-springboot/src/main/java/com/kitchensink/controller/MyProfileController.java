package com.kitchensink.controller;

import com.kitchensink.dto.Response;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.model.User;
import com.kitchensink.service.EmailService;
import com.kitchensink.service.OtpService;
import com.kitchensink.service.RoleService;
import com.kitchensink.service.UpdateRequestService;
import com.kitchensink.service.UserService;
import com.kitchensink.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/my-profile")
@Tag(name = "My Profile", description = "User's Own Profile Management API")
public class MyProfileController {
    
    private static final Logger logger = LoggerFactory.getLogger(MyProfileController.class);
    private final UserService userService;
    private final UpdateRequestService updateRequestService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RoleService roleService;
    
    public MyProfileController(UserService userService, UpdateRequestService updateRequestService,
                              OtpService otpService, EmailService emailService, RoleService roleService) {
        this.userService = userService;
        this.updateRequestService = updateRequestService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.roleService = roleService;
    }
    
    @GetMapping
    @Operation(summary = "Get my profile", description = "Get current user's own profile")
    public ResponseEntity<Response<UserResponseDTO>> getMyProfile(Authentication authentication) {
        logger.debug("Getting own profile");
        
        String userId = authentication.getName();
        User user = userService.getUserById(userId);
        UserResponseDTO responseDTO = mapToResponseDTO(user);
        
        Response<UserResponseDTO> response = Response.success(responseDTO, "Profile retrieved successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/phone")
    @Operation(summary = "Update my phone number", description = "User raises update request for phone number")
    public ResponseEntity<Response<Map<String, String>>> updateMyPhoneNumber(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Updating own phone number");
        
        String userId = authentication.getName();
        String newPhoneNumber = request.get("phoneNumber");
        
        if (newPhoneNumber == null || newPhoneNumber.isEmpty()) {
            Response<Map<String, String>> errorResponse = Response.error("Phone number is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        updateRequestService.createUpdateRequest(userId, "phoneNumber", user.getPhoneNumber(), newPhoneNumber);
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Update request created successfully");
        
        Response<Map<String, String>> response = Response.success(responseData, "Update request created");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/email/request-otp")
    @Operation(summary = "Request OTP for email change", description = "Request OTP to new email for email change verification")
    public ResponseEntity<Response<Map<String, String>>> requestEmailChangeOtp(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Email change OTP requested");
        
        String userId = authentication.getName();
        String newEmail = request.get("newEmail");
        
        if (newEmail == null || newEmail.isEmpty()) {
            Response<Map<String, String>> errorResponse = Response.error("New email is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Create OTP and send email
        com.kitchensink.model.Otp otp = otpService.createOtp(newEmail, "EMAIL_CHANGE");
        emailService.sendEmailChangeOtp(newEmail, otp.getOtpCode());
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "OTP sent to new email");
        responseData.put("otpId", otp.getId());
        
        Response<Map<String, String>> response = Response.success(responseData, "OTP sent successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/email")
    @Operation(summary = "Request email change", description = "User requests email change after OTP verification")
    public ResponseEntity<Response<Map<String, String>>> requestEmailChange(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Requesting email change");
        
        String userId = authentication.getName();
        String newEmail = request.get("newEmail");
        String otp = request.get("otp");
        String otpId = request.get("otpId");
        
        if (newEmail == null || newEmail.isEmpty() || otp == null || otp.isEmpty()) {
            Response<Map<String, String>> errorResponse = Response.error("New email and OTP are required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Verify OTP
        boolean isValid = otpService.verifyOtp(newEmail, otp, "EMAIL_CHANGE");
        if (!isValid) {
            Response<Map<String, String>> errorResponse = Response.error("Invalid OTP", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        String oldEmail = user.getEmail();
        
        // User must raise update request
        updateRequestService.createEmailChangeRequest(userId, oldEmail, newEmail, otpId);
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Email change request created successfully");
        
        Response<Map<String, String>> response = Response.success(responseData, "Email change request created");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/update-request")
    @Operation(summary = "Raise update request", description = "User raises update request for profile fields")
    public ResponseEntity<Response<Map<String, String>>> raiseUpdateRequest(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Update request raised");
        
        String userId = authentication.getName();
        String fieldName = request.get("fieldName");
        String newValue = request.get("newValue");
        
        if (fieldName == null || newValue == null) {
            Response<Map<String, String>> errorResponse = Response.error("Field name and new value are required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        String oldValue = null;
        
        if ("name".equals(fieldName)) {
            oldValue = user.getName();
        } else if ("phoneNumber".equals(fieldName)) {
            oldValue = user.getPhoneNumber();
        } else {
            Response<Map<String, String>> errorResponse = Response.error("Invalid field name", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        updateRequestService.createUpdateRequest(userId, fieldName, oldValue, newValue);
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Update request created successfully");
        
        Response<Map<String, String>> response = Response.success(responseData, "Update request created");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/update-requests")
    @Operation(summary = "Get my update requests", description = "Get all update requests for current user")
    public ResponseEntity<Response<List<com.kitchensink.model.UpdateRequest>>> getMyUpdateRequests(
            Authentication authentication) {
        
        logger.debug("Getting own update requests");
        
        String userId = authentication.getName();
        List<com.kitchensink.model.UpdateRequest> requests = updateRequestService.getUserRequests(userId);
        
        Response<List<com.kitchensink.model.UpdateRequest>> response = Response.success(requests, "Update requests retrieved successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/update-requests/{requestId}")
    @Operation(summary = "Revoke update request", description = "User revokes/cancels their own pending update request")
    public ResponseEntity<Response<Map<String, String>>> revokeUpdateRequest(
            @PathVariable String requestId,
            Authentication authentication) {
        
        logger.debug("Revoking update request: {}", requestId);
        
        String userId = authentication.getName();
        updateRequestService.revokeRequest(requestId, userId);
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Update request revoked successfully");
        
        Response<Map<String, String>> response = Response.success(responseData, "Update request revoked");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setIsdCode(user.getIsdCode());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setCountry(user.getCountry());
        dto.setRole(roleService.getRoleNameByUserId(user.getId()));
        dto.setRegistrationDate(user.getRegistrationDate());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setStatus(user.getStatus());
        return dto;
    }
}

