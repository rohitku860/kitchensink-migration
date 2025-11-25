package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Document(collection = "update_requests")
public class UpdateRequest {
    
    @Id
    private String id;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Request type is required")
    private String requestType; // PROFILE_UPDATE, EMAIL_CHANGE
    
    private String fieldName; // name, phoneNumber, email, etc.
    
    @Transient
    private String oldValue; // Plain text old value (not persisted)
    
    @Transient
    private String newValue; // Plain text new value (not persisted)
    
    // Encrypted values for storage
    private String oldValueEncrypted;
    private String newValueEncrypted;
    
    private String status; // PENDING, APPROVED, REJECTED
    
    private String requestedBy; // User ID who requested
    
    private String reviewedBy; // Admin ID who reviewed (if applicable)
    
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    
    private String rejectionReason; // If rejected
    
    // For email change - OTP verification status
    private boolean otpVerified = false;
    private String otpId; // Reference to OTP used for verification
    
    // Constructors
    public UpdateRequest() {
        this.requestedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.otpVerified = false;
    }
    
    public UpdateRequest(String userId, String requestType, String fieldName, String oldValue, String newValue) {
        this();
        this.userId = userId;
        this.requestType = requestType;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.requestedBy = userId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRequestType() {
        return requestType;
    }
    
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getOldValueEncrypted() {
        return oldValueEncrypted;
    }
    
    public void setOldValueEncrypted(String oldValueEncrypted) {
        this.oldValueEncrypted = oldValueEncrypted;
    }
    
    public String getNewValueEncrypted() {
        return newValueEncrypted;
    }
    
    public void setNewValueEncrypted(String newValueEncrypted) {
        this.newValueEncrypted = newValueEncrypted;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    public String getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public boolean isOtpVerified() {
        return otpVerified;
    }
    
    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }
    
    public String getOtpId() {
        return otpId;
    }
    
    public void setOtpId(String otpId) {
        this.otpId = otpId;
    }
}

