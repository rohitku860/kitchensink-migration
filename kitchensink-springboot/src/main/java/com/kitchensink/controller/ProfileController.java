package com.kitchensink.controller;

import com.kitchensink.dto.Response;
import com.kitchensink.dto.UserRequestDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import com.kitchensink.service.EmailService;
import com.kitchensink.service.OtpService;
import com.kitchensink.service.RoleService;
import com.kitchensink.service.UpdateRequestService;
import com.kitchensink.service.UserService;
import com.kitchensink.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/v1/profile")
@Tag(name = "Profile", description = "User Profile API")
public class ProfileController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;
    private final UpdateRequestService updateRequestService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RoleService roleService;
    
    public ProfileController(UserService userService, UpdateRequestService updateRequestService,
                            OtpService otpService, EmailService emailService, RoleService roleService) {
        this.userService = userService;
        this.updateRequestService = updateRequestService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.roleService = roleService;
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile", description = "Get user profile by ID. Admin can access any profile, User can only access own profile.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> getProfile(@PathVariable String userId, Authentication authentication) {
        logger.debug("Getting profile for user ID: {}", userId);
        
        // Check access: Admin can access any, User can only access own
        String currentUserId = authentication.getName();
        
        if (!roleService.isAdminByUserId(currentUserId) && !currentUserId.equals(userId)) {
            logger.warn("User {} attempted to access profile of user {}", currentUserId, userId);
            Response<UserResponseDTO> errorResponse = Response.error("Forbidden: You can only access your own profile", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        UserResponseDTO responseDTO = mapToResponseDTO(user);
        
        Response<UserResponseDTO> response = Response.success(responseDTO, "Profile retrieved successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}/name")
    @Operation(summary = "Update name", description = "Update name. Admin can update directly, User must raise request.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> updateName(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Updating name for user ID: {}", userId);
        
        String currentUserId = authentication.getName();
        String newName = request.get("name");
        
        if (newName == null || newName.isEmpty()) {
            Response<UserResponseDTO> errorResponse = Response.error("Name is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Admin can update directly (including own name)
        if (roleService.isAdminByUserId(currentUserId)) {
            User user = userService.updateUser(userId, newName, null, null);
            UserResponseDTO responseDTO = mapToResponseDTO(user);
            Response<UserResponseDTO> response = Response.success(responseDTO, "Name updated successfully");
            response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.ok(response);
        }
        
        // User must raise update request
        if (!currentUserId.equals(userId)) {
            Response<UserResponseDTO> errorResponse = Response.error("Forbidden", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        updateRequestService.createUpdateRequest(userId, "name", user.getName(), newName);
        
        Response<UserResponseDTO> response = Response.success(null, "Update request created successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}/phone")
    @Operation(summary = "Update phone number", description = "Update phone number. Admin can update directly, User must raise request.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> updatePhoneNumber(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Updating phone number for user ID: {}", userId);
        
        String currentUserId = authentication.getName();
        String newPhoneNumber = request.get("phoneNumber");
        
        if (newPhoneNumber == null || newPhoneNumber.isEmpty()) {
            Response<UserResponseDTO> errorResponse = Response.error("Phone number is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Admin can update directly (including own phone)
        if (roleService.isAdminByUserId(currentUserId)) {
            User user = userService.updateUserPhoneNumber(userId, newPhoneNumber);
            UserResponseDTO responseDTO = mapToResponseDTO(user);
            Response<UserResponseDTO> response = Response.success(responseDTO, "Phone number updated successfully");
            response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.ok(response);
        }
        
        // User must raise update request
        if (!currentUserId.equals(userId)) {
            Response<UserResponseDTO> errorResponse = Response.error("Forbidden", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        updateRequestService.createUpdateRequest(userId, "phoneNumber", user.getPhoneNumber(), newPhoneNumber);
        
        Response<UserResponseDTO> response = Response.success(null, "Update request created successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{userId}/email/request-otp")
    @Operation(summary = "Request OTP for email change", description = "Request OTP to new email for email change verification")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<Map<String, String>>> requestEmailChangeOtp(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Email change OTP requested for user ID: {}", userId);
        
        String currentUserId = authentication.getName();
        String newEmail = request.get("newEmail");
        
        if (newEmail == null || newEmail.isEmpty()) {
            Response<Map<String, String>> errorResponse = Response.error("New email is required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Check access
        if (!roleService.isAdminByUserId(currentUserId) && !currentUserId.equals(userId)) {
            Response<Map<String, String>> errorResponse = Response.error("Forbidden", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
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
    
    @PutMapping("/{userId}/email")
    @Operation(summary = "Update email", description = "Update email. Admin can update directly after OTP, User must raise request after OTP.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> updateEmail(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Updating email for user ID: {}", userId);
        
        String currentUserId = authentication.getName();
        String newEmail = request.get("newEmail");
        String otp = request.get("otp");
        String otpId = request.get("otpId");
        
        if (newEmail == null || newEmail.isEmpty() || otp == null || otp.isEmpty()) {
            Response<UserResponseDTO> errorResponse = Response.error("New email and OTP are required", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Verify OTP
        boolean isValid = otpService.verifyOtp(newEmail, otp, "EMAIL_CHANGE");
        if (!isValid) {
            Response<UserResponseDTO> errorResponse = Response.error("Invalid OTP", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Check access
        if (!roleService.isAdminByUserId(currentUserId) && !currentUserId.equals(userId)) {
            Response<UserResponseDTO> errorResponse = Response.error("Forbidden", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        User user = userService.getUserById(userId);
        String oldEmail = user.getEmail();
        
        // Admin can update directly (including own email)
        if (roleService.isAdminByUserId(currentUserId)) {
            User updated = userService.updateUserEmail(userId, newEmail);
            emailService.sendEmailChangeConfirmation(oldEmail, newEmail);
            
            UserResponseDTO responseDTO = mapToResponseDTO(updated);
            Response<UserResponseDTO> response = Response.success(responseDTO, "Email updated successfully");
            response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.ok(response);
        }
        
        // User must raise update request
        updateRequestService.createEmailChangeRequest(userId, oldEmail, newEmail, otpId);
        
        Response<UserResponseDTO> response = Response.success(null, "Email change request created successfully");
        response.setCorrelationId(CorrelationIdUtil.getCorrelationId());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{userId}/update-request")
    @Operation(summary = "Raise update request", description = "User raises update request for profile fields")
    @PreAuthorize("authentication.name == #userId")
    public ResponseEntity<Response<Map<String, String>>> raiseUpdateRequest(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        
        logger.debug("Update request raised for user ID: {}", userId);
        
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
    
    @GetMapping("/{userId}/update-requests")
    @Operation(summary = "Get user update requests", description = "Get all update requests for a user")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<List<UpdateRequest>>> getUserUpdateRequests(
            @PathVariable String userId,
            Authentication authentication) {
        
        String currentUserId = authentication.getName();
        
        if (!roleService.isAdminByUserId(currentUserId) && !currentUserId.equals(userId)) {
            Response<List<UpdateRequest>> errorResponse = Response.error("Forbidden", null);
            errorResponse.setCorrelationId(CorrelationIdUtil.getCorrelationId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        List<UpdateRequest> requests = updateRequestService.getUserRequests(userId);
        
        Response<List<UpdateRequest>> response = Response.success(requests, "Update requests retrieved successfully");
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

