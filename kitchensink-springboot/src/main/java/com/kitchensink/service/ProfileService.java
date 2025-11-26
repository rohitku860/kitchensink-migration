package com.kitchensink.service;

import com.kitchensink.dto.FieldUpdateRequestDTO;
import com.kitchensink.dto.UpdateRequestResponseDTO;
import com.kitchensink.dto.UserResponseDTO;
import com.kitchensink.model.UpdateRequest;
import com.kitchensink.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProfileService {
    
    private final UserService userService;
    private final UpdateRequestService updateRequestService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RoleService roleService;
    
    public ProfileService(UserService userService, UpdateRequestService updateRequestService,
                         OtpService otpService, EmailService emailService, RoleService roleService) {
        this.userService = userService;
        this.updateRequestService = updateRequestService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.roleService = roleService;
    }
    
    public UserResponseDTO getProfile(String userId) {
        User user = userService.getUserById(userId);
        return mapToResponseDTO(user);
    }
    
    public UserResponseDTO updateFields(String userId, List<FieldUpdateRequestDTO> fieldUpdates, boolean isAdmin) {
        if (fieldUpdates == null || fieldUpdates.isEmpty()) {
            throw new com.kitchensink.exception.ResourceConflictException("At least one field update is required", "fieldUpdates");
        }
        
        fieldUpdates.forEach(this::validateFieldUpdate);
        
        if (isAdmin) {
            return processAdminUpdates(userId, fieldUpdates);
        } else {
            fieldUpdates.forEach(update -> processUserUpdateRequest(userId, update));
            return null;
        }
    }
    
    private UserResponseDTO processAdminUpdates(String userId, List<FieldUpdateRequestDTO> fieldUpdates) {
        User existingUser = userService.getUserById(userId);
        String name = existingUser.getName();
        String email = existingUser.getEmail();
        String isdCode = existingUser.getIsdCode();
        String phoneNumber = existingUser.getPhoneNumber();
        String dateOfBirth = existingUser.getDateOfBirth();
        String address = existingUser.getAddress();
        String city = existingUser.getCity();
        String country = existingUser.getCountry();
        
        String oldEmail = existingUser.getEmail();
        boolean hasEmailUpdate = false;
        
        for (FieldUpdateRequestDTO update : fieldUpdates) {
            String fieldName = update.getFieldName();
            String newValue = update.getValue();
            
            if ("email".equalsIgnoreCase(fieldName)) {
                hasEmailUpdate = true;
                email = newValue;
                verifyEmailOtp(update);
            } else {
                switch (fieldName.toLowerCase()) {
                    case "name":
                        name = newValue;
                        break;
                    case "phonenumber":
                    case "phone":
                        phoneNumber = newValue;
                        break;
                    case "isdcode":
                    case "isd":
                        isdCode = newValue;
                        break;
                    case "dateofbirth":
                    case "dob":
                        dateOfBirth = newValue;
                        break;
                    case "address":
                        address = newValue;
                        break;
                    case "city":
                        city = newValue;
                        break;
                    case "country":
                        country = newValue;
                        break;
                    default:
                        throw new com.kitchensink.exception.ResourceConflictException("Invalid field name: " + fieldName, "fieldName");
                }
            }
        }
        
        User updated = userService.updateUser(userId, name, email, isdCode, phoneNumber, dateOfBirth, address, city, country);
        
        if (hasEmailUpdate) {
            emailService.sendEmailChangeConfirmation(oldEmail, email);
        }
        
        return mapToResponseDTO(updated);
    }
    
    private void processUserUpdateRequest(String userId, FieldUpdateRequestDTO update) {
        String fieldName = update.getFieldName();
        String newValue = update.getValue();
        
        if ("email".equalsIgnoreCase(fieldName)) {
            verifyEmailOtp(update);
            updateRequestService.createEmailChangeRequest(userId, newValue);
        } else {
            updateRequestService.createUpdateRequest(userId, fieldName, newValue);
        }
    }
    
    private void verifyEmailOtp(FieldUpdateRequestDTO update) {
        String otp = update.getOtp();
        if (otp == null || otp.isEmpty()) {
            throw new com.kitchensink.exception.ResourceConflictException("OTP is required for email changes", "otp");
        }
        
        boolean isValid = otpService.verifyOtp(update.getValue(), otp, "EMAIL_CHANGE");
        if (!isValid) {
            throw new com.kitchensink.exception.ResourceConflictException("Invalid OTP", "otp");
        }
    }
    
    private void validateFieldUpdate(FieldUpdateRequestDTO update) {
        String fieldName = update.getFieldName();
        String newValue = update.getValue();
        
        if (fieldName == null || fieldName.isEmpty()) {
            throw new com.kitchensink.exception.ResourceConflictException("Field name is required", "fieldName");
        }
        
        if (newValue == null || newValue.isEmpty()) {
            throw new com.kitchensink.exception.ResourceConflictException("New value is required for field: " + fieldName, "value");
        }
    }
    
    
    public Map<String, String> requestEmailChangeOtp(String newEmail) {
        if (newEmail == null || newEmail.isEmpty()) {
            throw new com.kitchensink.exception.ResourceConflictException("New email is required", "newEmail");
        }
        
        com.kitchensink.model.Otp otp = otpService.createOtp(newEmail, "EMAIL_CHANGE");
        emailService.sendEmailChangeOtp(newEmail, otp.getOtpCode());
        
        return Map.of(
            "message", "OTP sent to new email",
            "otpId", otp.getId()
        );
    }
    
    
    public List<UpdateRequestResponseDTO> getUserUpdateRequests(String userId) {
        List<UpdateRequest> requests = updateRequestService.getUserRequests(userId);
        return updateRequestService.mapToUpdateRequestDTOs(requests);
    }
    
    public void revokeUpdateRequest(String requestId, String userId) {
        updateRequestService.revokeRequest(requestId, userId);
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

