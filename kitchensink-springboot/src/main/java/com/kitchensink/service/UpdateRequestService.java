package com.kitchensink.service;

import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import com.kitchensink.repository.UpdateRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UpdateRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateRequestService.class);
    private final UpdateRequestRepository updateRequestRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    
    public UpdateRequestService(UpdateRequestRepository updateRequestRepository,
                               UserService userService, EmailService emailService,
                               EncryptionService encryptionService, AuditService auditService) {
        this.updateRequestRepository = updateRequestRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }
    
    /**
     * Create update request
     */
    public UpdateRequest createUpdateRequest(String userId, String fieldName, String oldValue, String newValue) {
        logger.debug("Creating update request for user ID: {}, field: {}", userId, fieldName);
        
        UpdateRequest request = new UpdateRequest(userId, "PROFILE_UPDATE", fieldName, oldValue, newValue);
        request.setOldValueEncrypted(encryptionService.encrypt(oldValue));
        request.setNewValueEncrypted(encryptionService.encrypt(newValue));
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        // Notify admin
        User admin = userService.getUserByEmail("rohitku860@gmail.com")
                .orElse(null);
        if (admin != null) {
            User user = userService.getUserById(userId);
            emailService.sendUpdateRequestNotification(admin.getEmail(), user.getName(), fieldName);
        }
        
        logger.info("Update request created successfully: {}", saved.getId());
        return saved;
    }
    
    /**
     * Create email change request (after OTP verification)
     */
    public UpdateRequest createEmailChangeRequest(String userId, String oldEmail, String newEmail, String otpId) {
        logger.debug("Creating email change request for user ID: {}", userId);
        
        UpdateRequest request = new UpdateRequest(userId, "EMAIL_CHANGE", "email", oldEmail, newEmail);
        request.setOldValueEncrypted(encryptionService.encrypt(oldEmail));
        request.setNewValueEncrypted(encryptionService.encrypt(newEmail));
        request.setOtpVerified(true);
        request.setOtpId(otpId);
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        // Notify admin
        User admin = userService.getUserByEmail("rohitku860@gmail.com")
                .orElse(null);
        if (admin != null) {
            User user = userService.getUserById(userId);
            emailService.sendUpdateRequestNotification(admin.getEmail(), user.getName(), "email");
        }
        
        logger.info("Email change request created successfully: {}", saved.getId());
        return saved;
    }
    
    /**
     * Get all pending requests
     */
    public List<UpdateRequest> getPendingRequests() {
        logger.debug("Fetching all pending update requests");
        List<UpdateRequest> requests = updateRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
        
        // Decrypt values
        requests.forEach(request -> {
            if (request.getOldValueEncrypted() != null) {
                request.setOldValue(encryptionService.decrypt(request.getOldValueEncrypted()));
            }
            if (request.getNewValueEncrypted() != null) {
                request.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
            }
        });
        
        return requests;
    }
    
    /**
     * Get requests for a user
     */
    public List<UpdateRequest> getUserRequests(String userId) {
        logger.debug("Fetching update requests for user ID: {}", userId);
        List<UpdateRequest> requests = updateRequestRepository.findByUserIdOrderByRequestedAtDesc(userId);
        
        // Decrypt values
        requests.forEach(request -> {
            if (request.getOldValueEncrypted() != null) {
                request.setOldValue(encryptionService.decrypt(request.getOldValueEncrypted()));
            }
            if (request.getNewValueEncrypted() != null) {
                request.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
            }
        });
        
        return requests;
    }
    
    /**
     * Approve update request
     */
    public UpdateRequest approveRequest(String requestId, String adminId) {
        logger.debug("Approving update request: {}", requestId);
        
        UpdateRequest request = updateRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UpdateRequest", requestId));
        
        // Decrypt values
        if (request.getOldValueEncrypted() != null) {
            request.setOldValue(encryptionService.decrypt(request.getOldValueEncrypted()));
        }
        if (request.getNewValueEncrypted() != null) {
            request.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
        }
        
        // Update user based on field
        User user = userService.getUserById(request.getUserId());
        String fieldName = request.getFieldName();
        String newValue = request.getNewValue();
        
        if ("email".equals(fieldName)) {
            userService.updateUserEmail(request.getUserId(), newValue);
            emailService.sendEmailChangeConfirmation(request.getOldValue(), newValue);
        } else if ("phoneNumber".equals(fieldName)) {
            userService.updateUserPhoneNumber(request.getUserId(), newValue);
        } else if ("name".equals(fieldName)) {
            userService.updateUser(request.getUserId(), newValue, null, null);
        } else {
            userService.updateUser(request.getUserId(), null, null, newValue);
        }
        
        // Update request status
        request.setStatus("APPROVED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        // Notify user
        user = userService.getUserById(request.getUserId());
        emailService.sendUpdateRequestApproval(user.getEmail(), user.getName(), fieldName);
        
        // Log audit
        auditService.logUpdateRequestApproved(requestId, request.getUserId(), fieldName, adminId);
        
        logger.info("Update request approved: {}", requestId);
        return saved;
    }
    
    /**
     * Reject update request
     */
    public UpdateRequest rejectRequest(String requestId, String adminId, String reason) {
        logger.debug("Rejecting update request: {}", requestId);
        
        UpdateRequest request = updateRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UpdateRequest", requestId));
        
        // Decrypt values for notification
        if (request.getNewValueEncrypted() != null) {
            request.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
        }
        
        request.setStatus("REJECTED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        // Notify user
        User user = userService.getUserById(request.getUserId());
        emailService.sendUpdateRequestRejection(user.getEmail(), user.getName(), request.getFieldName(), reason);
        
        // Log audit
        auditService.logUpdateRequestRejected(requestId, request.getUserId(), request.getFieldName(), adminId, reason);
        
        logger.info("Update request rejected: {}", requestId);
        return saved;
    }
    
    /**
     * Revoke/Cancel update request (only for pending requests by the user who created it)
     */
    public void revokeRequest(String requestId, String userId) {
        logger.debug("Revoking update request: {} by user: {}", requestId, userId);
        
        UpdateRequest request = updateRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UpdateRequest", requestId));
        
        // Verify the request belongs to the user
        if (!request.getUserId().equals(userId)) {
            throw new com.kitchensink.exception.ResourceConflictException(
                    "You can only revoke your own update requests", "requestId");
        }
        
        // Only allow revoking pending requests
        if (!"PENDING".equals(request.getStatus())) {
            throw new com.kitchensink.exception.ResourceConflictException(
                    "Only pending requests can be revoked", "status");
        }
        
        // Get field name before deletion for audit logging
        String fieldName = request.getFieldName();
        
        // Delete the request
        updateRequestRepository.delete(request);
        
        // Log audit
        auditService.logUpdateRequestRevoked(requestId, userId, fieldName);
        
        logger.info("Update request revoked: {} by user: {}", requestId, userId);
    }
}

