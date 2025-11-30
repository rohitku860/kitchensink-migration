package com.kitchensink.controller;

import com.kitchensink.dto.FieldUpdateRequestDTO;
import com.kitchensink.dto.Response;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.service.ProfileService;
import com.kitchensink.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/profile")
@Tag(name = "Profile", description = "User Profile API")
public class ProfileController {
    
    private final ProfileService profileService;
    private final RoleService roleService;
    
    public ProfileController(ProfileService profileService, RoleService roleService) {
        this.profileService = profileService;
        this.roleService = roleService;
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile", description = "Get user profile by ID. Admin can access any profile, User can only access own profile.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> getProfile(@PathVariable String userId) {
        UserResponseDTO responseDTO = profileService.getProfile(userId);
        Response<UserResponseDTO> response = Response.success(responseDTO, "Profile retrieved successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{userId}/email/request-otp")
    @Operation(summary = "Request OTP for email change", description = "Request OTP to new email for email change verification")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<Map<String, String>>> requestEmailChangeOtp(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        
        String newEmail = request.get("newEmail");
        
        try {
            Map<String, String> responseData = profileService.requestEmailChangeOtp(newEmail);
            Response<Map<String, String>> response = Response.success(responseData, "OTP sent successfully");
            return ResponseEntity.ok(response);
        } catch (com.kitchensink.exception.ResourceConflictException e) {
            Response<Map<String, String>> errorResponse = Response.error(e.getMessage(), null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update profile fields", description = "Update one or more profile fields in batch. Admin updates directly, User automatically creates update requests. For email, OTP is required.")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<UserResponseDTO>> updateFields(
            @PathVariable String userId,
            @RequestBody List<FieldUpdateRequestDTO> fieldUpdates,
            Authentication authentication) {
        
        boolean isAdmin = roleService.isAdminByUserId(authentication.getName());
        
        try {
            UserResponseDTO responseDTO = profileService.updateFields(userId, fieldUpdates, isAdmin);
            String message = isAdmin ? 
                String.format("%d field(s) updated successfully", fieldUpdates.size()) : 
                String.format("Update request(s) created successfully for %d field(s)", fieldUpdates.size());
            Response<UserResponseDTO> response = Response.success(responseDTO, message);
            return ResponseEntity.ok(response);
        } catch (com.kitchensink.exception.ResourceConflictException e) {
            Response<UserResponseDTO> errorResponse = Response.error(e.getMessage(), null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/{userId}/update-requests")
    @Operation(summary = "Get user update requests", description = "Get all update requests for a user")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public ResponseEntity<Response<List<UpdateRequestResponseDTO>>> getUserUpdateRequests(@PathVariable String userId) {
        List<UpdateRequestResponseDTO> requests = profileService.getUserUpdateRequests(userId);
        Response<List<UpdateRequestResponseDTO>> response = Response.success(requests, "Update requests retrieved successfully");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}/update-requests/{requestId}")
    @Operation(summary = "Revoke update request", description = "User revokes/cancels their own pending update request")
    @PreAuthorize("authentication.name == #userId")
    public ResponseEntity<Response<Map<String, String>>> revokeUpdateRequest(
            @PathVariable String userId,
            @PathVariable String requestId) {
        
        profileService.revokeUpdateRequest(requestId, userId);
        
        Map<String, String> responseData = Map.of("message", "Update request revoked successfully");
        Response<Map<String, String>> response = Response.success(responseData, "Update request revoked");
        return ResponseEntity.ok(response);
    }
}

