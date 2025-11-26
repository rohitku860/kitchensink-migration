package com.kitchensink.service;

import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import com.kitchensink.repository.UpdateRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UpdateRequestService {
    
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
     * Create update request for any field
     */
    public UpdateRequest createUpdateRequest(String userId, String fieldName, String newValue) {
        User user = userService.getUserById(userId);
        String oldValue = getFieldValue(user, fieldName);
        
        if (oldValue == null) {
            throw new com.kitchensink.exception.ResourceNotFoundException("Field", fieldName);
        }
        
        UpdateRequest request = new UpdateRequest(userId, "PROFILE_UPDATE", fieldName, oldValue, newValue);
        request.setOldValueEncrypted(encryptionService.encrypt(oldValue != null ? oldValue : ""));
        request.setNewValueEncrypted(encryptionService.encrypt(newValue));
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        User admin = userService.getUserByEmail("rohitku860@gmail.com")
                .orElse(null);
        if (admin != null) {
            emailService.sendUpdateRequestNotification(admin.getEmail(), user.getName(), fieldName);
        }
        
        return saved;
    }
    
    private String getFieldValue(User user, String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "name":
                return user.getName();
            case "email":
                if (user.getEmailEncrypted() != null) {
                    return user.getEmail() != null ? user.getEmail() : encryptionService.decrypt(user.getEmailEncrypted());
                }
                return user.getEmail();
            case "phonenumber":
            case "phone":
                if (user.getPhoneNumberEncrypted() != null) {
                    return user.getPhoneNumber() != null ? user.getPhoneNumber() : encryptionService.decrypt(user.getPhoneNumberEncrypted());
                }
                return user.getPhoneNumber();
            case "isdcode":
            case "isd":
                return user.getIsdCode();
            case "dateofbirth":
            case "dob":
                return user.getDateOfBirth();
            case "address":
                return user.getAddress();
            case "city":
                return user.getCity();
            case "country":
                return user.getCountry();
            default:
                return null;
        }
    }
    
    private void updateUserField(String userId, String fieldName, String newValue) {
        switch (fieldName.toLowerCase()) {
            case "email":
                userService.updateUserEmail(userId, newValue);
                break;
            case "phonenumber":
            case "phone":
                userService.updateUserPhoneNumber(userId, newValue);
                break;
            case "name":
                userService.updateUser(userId, newValue, null, null);
                break;
            case "isdcode":
            case "isd":
                userService.updateUser(userId, null, null, newValue, null, null, null, null, null);
                break;
            case "dateofbirth":
            case "dob":
                userService.updateUser(userId, null, null, null, null, newValue, null, null, null);
                break;
            case "address":
                userService.updateUser(userId, null, null, null, null, null, newValue, null, null);
                break;
            case "city":
                userService.updateUser(userId, null, null, null, null, null, null, newValue, null);
                break;
            case "country":
                userService.updateUser(userId, null, null, null, null, null, null, null, newValue);
                break;
            default:
                throw new com.kitchensink.exception.ResourceConflictException("Invalid field name: " + fieldName, "fieldName");
        }
    }
    
    public UpdateRequest createEmailChangeRequest(String userId, String newEmail) {
        User user = userService.getUserById(userId);
        String oldEmail = getFieldValue(user, "email");
        
        UpdateRequest request = new UpdateRequest(userId, "EMAIL_CHANGE", "email", oldEmail, newEmail);
        request.setOldValueEncrypted(encryptionService.encrypt(oldEmail != null ? oldEmail : ""));
        request.setNewValueEncrypted(encryptionService.encrypt(newEmail));
        request.setOtpVerified(true);
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        User admin = userService.getUserByEmail("rohitku860@gmail.com")
                .orElse(null);
        if (admin != null) {
            emailService.sendUpdateRequestNotification(admin.getEmail(), user.getName(), "email");
        }
        
        return saved;
    }
    
    public List<UpdateRequest> getPendingRequests() {
        List<UpdateRequest> requests = updateRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
        
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
    
    public List<UpdateRequest> getUserRequests(String userId) {
        List<UpdateRequest> requests = updateRequestRepository.findByUserIdOrderByRequestedAtDesc(userId);
        
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
    
    public UpdateRequestResponseDTO approveRequest(String requestId, String adminId) {
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
        
        updateUserField(request.getUserId(), fieldName, newValue);
        
        if ("email".equalsIgnoreCase(fieldName)) {
            emailService.sendEmailChangeConfirmation(request.getOldValue(), newValue);
        }
        
        request.setStatus("APPROVED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        user = userService.getUserById(request.getUserId());
        emailService.sendUpdateRequestApproval(user.getEmail(), user.getName(), fieldName);
        auditService.logUpdateRequestApproved(requestId, request.getUserId(), fieldName, adminId);
        
        return mapToUpdateRequestDTO(saved);
    }
    
    public UpdateRequestResponseDTO rejectRequest(String requestId, String adminId, String reason) {
        UpdateRequest request = updateRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UpdateRequest", requestId));
        
        if (request.getNewValueEncrypted() != null) {
            request.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
        }
        
        request.setStatus("REJECTED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        
        UpdateRequest saved = updateRequestRepository.save(request);
        
        User user = userService.getUserById(request.getUserId());
        emailService.sendUpdateRequestRejection(user.getEmail(), user.getName(), request.getFieldName(), reason);
        auditService.logUpdateRequestRejected(requestId, request.getUserId(), request.getFieldName(), adminId, reason);
        
        return mapToUpdateRequestDTO(saved);
    }
    
    /**
     * Map UpdateRequest entity to DTO
     */
    private UpdateRequestResponseDTO mapToUpdateRequestDTO(UpdateRequest request) {
        UpdateRequestResponseDTO dto = new UpdateRequestResponseDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUserId());
        dto.setRequestType(request.getRequestType());
        dto.setFieldName(request.getFieldName());
        dto.setStatus(request.getStatus());
        
        // Decrypt values for response
        if (request.getOldValueEncrypted() != null) {
            dto.setOldValue(encryptionService.decrypt(request.getOldValueEncrypted()));
        } else {
            dto.setOldValue(request.getOldValue());
        }
        
        if (request.getNewValueEncrypted() != null) {
            dto.setNewValue(encryptionService.decrypt(request.getNewValueEncrypted()));
        } else {
            dto.setNewValue(request.getNewValue());
        }
        
        dto.setRequestedAt(request.getRequestedAt());
        dto.setReviewedAt(request.getReviewedAt());
        dto.setReviewedBy(request.getReviewedBy());
        dto.setRejectionReason(request.getRejectionReason());
        
        return dto;
    }
    
    public List<UpdateRequestResponseDTO> mapToUpdateRequestDTOs(List<UpdateRequest> requests) {
        return requests.stream()
                .map(this::mapToUpdateRequestDTO)
                .collect(Collectors.toList());
    }
    
    public void revokeRequest(String requestId, String userId) {
        UpdateRequest request = updateRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UpdateRequest", requestId));
        
        if (!request.getUserId().equals(userId)) {
            throw new com.kitchensink.exception.ResourceConflictException(
                    "You can only revoke your own update requests", "requestId");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new com.kitchensink.exception.ResourceConflictException(
                    "Only pending requests can be revoked", "status");
        }
        
        String fieldName = request.getFieldName();
        updateRequestRepository.delete(request);
        auditService.logUpdateRequestRevoked(requestId, userId, fieldName);
    }
}

