package com.kitchensink.controller;

import com.kitchensink.dto.Response;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserRequestDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.model.User;
import com.kitchensink.model.UserRoleType;
import com.kitchensink.service.EmailService;
import com.kitchensink.service.RoleService;
import com.kitchensink.service.UpdateRequestService;
import com.kitchensink.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Admin", description = "Admin Dashboard API")
// Note: ADMIN role requirement is enforced at URL level in SecurityConfig
// All endpoints under /v1/admin/** require ADMIN role via .requestMatchers("/v1/admin/**").hasRole("ADMIN")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final UserService userService;
    private final UpdateRequestService updateRequestService;
    private final EmailService emailService;
    private final RoleService roleService;
    
    public AdminController(UserService userService, UpdateRequestService updateRequestService,
                          EmailService emailService, RoleService roleService) {
        this.userService = userService;
        this.updateRequestService = updateRequestService;
        this.emailService = emailService;
        this.roleService = roleService;
    }
    
    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Get paginated list of all users using cursor-based pagination, sorted by name")
    public ResponseEntity<Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String direction) {
        
        com.kitchensink.enums.Direction directionEnum = com.kitchensink.enums.Direction.fromString(direction);
        
        logger.debug("Admin fetching all users (excluding admins) - cursor: {}, size: {}, direction: {}", 
                cursor, size, directionEnum);
        
            com.kitchensink.dto.CursorPageResponse<User> cursorPage = 
                    userService.getAllUsersExcludingAdminsCursor(cursor, size, directionEnum);
            
            com.kitchensink.dto.CursorPageResponse<UserResponseDTO> responseDTOs = 
                    new com.kitchensink.dto.CursorPageResponse<>(
                            cursorPage.getContent().stream()
                                    .map(this::mapToResponseDTO)
                                    .collect(Collectors.toList()),
                            cursorPage.getNextCursor(),
                            cursorPage.getPreviousCursor(),
                            cursorPage.isHasNext(),
                            cursorPage.isHasPrevious(),
                            cursorPage.getSize(),
                            cursorPage.getTotalElements(),
                            cursorPage.getTotalPages(),
                            cursorPage.getNumber(),
                            cursorPage.getNextScrollId(),
                            cursorPage.getPrevScrollId()
                    );
            
            Response<com.kitchensink.dto.CursorPageResponse<UserResponseDTO>> response = 
                    Response.success(responseDTOs, "Users retrieved successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/search")
    @Operation(summary = "Search users by name", description = "Search users by name (case-insensitive)")
    public ResponseEntity<Response<List<UserResponseDTO>>> searchUsers(@RequestParam String name) {
        
        logger.debug("Admin searching users (excluding admins) by name: {}", name);
        
        List<User> users = userService.searchUsersByNameExcludingAdmins(name);
        List<UserResponseDTO> responseDTOs = users.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        
        Response<List<UserResponseDTO>> response = Response.success(responseDTOs, 
                String.format("Found %d users", responseDTOs.size()));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/users")
    @Operation(summary = "Create new user", description = "Admin creates a new user with USER role")
    public ResponseEntity<Response<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO requestDTO) {
        
        logger.debug("Admin creating new user");
        
        
        User user = userService.createUser(
                requestDTO.getName(),
                requestDTO.getEmail(),
                requestDTO.getIsdCode(),
                requestDTO.getPhoneNumber(),
                UserRoleType.USER.getName(),
                requestDTO.getDateOfBirth(),
                requestDTO.getAddress(),
                requestDTO.getCity(),
                requestDTO.getCountry()
        );
        
        UserResponseDTO responseDTO = mapToResponseDTO(user);
        
        // Send account creation email
        emailService.sendUserCreationEmail(user.getEmail(), user.getName());
        
        Response<UserResponseDTO> response = Response.success(responseDTO, "User created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user", description = "Admin updates any user's details")
    public ResponseEntity<Response<UserResponseDTO>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserRequestDTO requestDTO) {
        
        logger.debug("Admin updating user ID: {}", userId);
        
        User user = userService.updateUser(
                userId,
                requestDTO.getName(),
                requestDTO.getEmail(),
                requestDTO.getIsdCode(),
                requestDTO.getPhoneNumber(),
                requestDTO.getDateOfBirth(),
                requestDTO.getAddress(),
                requestDTO.getCity(),
                requestDTO.getCountry()
        );
        
        UserResponseDTO responseDTO = mapToResponseDTO(user);
        
        // Send update notification email
        emailService.sendUserUpdateNotification(user.getEmail(), user.getName());
        
        Response<UserResponseDTO> response = Response.success(responseDTO, "User updated successfully");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Admin deletes a user")
    public ResponseEntity<Response<Void>> deleteUser(@PathVariable String userId) {
        
        logger.debug("Admin deleting user ID: {}", userId);
        
        // Get user email before deletion for notification
        User user = userService.getUserById(userId);
        String userEmail = user.getEmail();
        String userName = user.getName();
        
        userService.deleteUser(userId);
        
        // Send deletion notification email
        emailService.sendUserDeletionNotification(userEmail, userName);
        
        Response<Void> response = Response.success(null, "User deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/update-requests")
    @Operation(summary = "Get all pending update requests", description = "Get all pending update requests for admin review")
    public ResponseEntity<Response<List<UpdateRequestResponseDTO>>> getPendingUpdateRequests() {
        
        logger.debug("Admin fetching pending update requests");
        
        List<com.kitchensink.model.UpdateRequest> requests = updateRequestService.getPendingRequests();
        List<UpdateRequestResponseDTO> requestDTOs = updateRequestService.mapToUpdateRequestDTOs(requests);
        
        Response<List<UpdateRequestResponseDTO>> response = Response.success(requestDTOs, "Update requests retrieved successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/update-requests/{requestId}/approve")
    @Operation(summary = "Approve update request", description = "Admin approves an update request")
    public ResponseEntity<Response<UpdateRequestResponseDTO>> approveUpdateRequest(
            @PathVariable String requestId,
            Authentication authentication) {
        
        logger.debug("Admin approving update request: {}", requestId);
        
        String adminId = authentication.getName();
        UpdateRequestResponseDTO request = updateRequestService.approveRequest(requestId, adminId);
        
        Response<UpdateRequestResponseDTO> response = Response.success(request, "Update request approved successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/update-requests/{requestId}/reject")
    @Operation(summary = "Reject update request", description = "Admin rejects an update request")
    public ResponseEntity<Response<UpdateRequestResponseDTO>> rejectUpdateRequest(
            @PathVariable String requestId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.debug("Admin rejecting update request: {}", requestId);
        
        String adminId = authentication.getName();
        String reason = request.getOrDefault("reason", "No reason provided");
        
        UpdateRequestResponseDTO rejectedRequest = updateRequestService.rejectRequest(requestId, adminId, reason);
        
        Response<UpdateRequestResponseDTO> response = Response.success(rejectedRequest, "Update request rejected");
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

