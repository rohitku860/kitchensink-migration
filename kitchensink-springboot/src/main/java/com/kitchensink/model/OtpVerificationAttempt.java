package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Document(collection = "otp_verification_attempts")
@CompoundIndex(name = "emailHash_purpose_idx", def = "{'emailHash': 1, 'purpose': 1}", unique = true)
public class OtpVerificationAttempt {
    
    @Id
    private String id;
    
    @NotBlank(message = "Email hash is required")
    @Indexed
    private String emailHash;
    
    @NotBlank(message = "Purpose is required")
    @Indexed
    private String purpose; // LOGIN, EMAIL_CHANGE, etc.
    
    private int failedAttempts = 0;
    
    private LocalDateTime lockoutUntil;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastAttemptAt;
    
    private boolean isLockedOut = false;
    
    public OtpVerificationAttempt() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.failedAttempts = 0;
        this.isLockedOut = false;
    }
    
    public OtpVerificationAttempt(String emailHash, String purpose) {
        this();
        this.emailHash = emailHash;
        this.purpose = purpose;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmailHash() {
        return emailHash;
    }
    
    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public int getFailedAttempts() {
        return failedAttempts;
    }
    
    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
    
    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.lastAttemptAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getLockoutUntil() {
        return lockoutUntil;
    }
    
    public void setLockoutUntil(LocalDateTime lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
        this.isLockedOut = lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }
    
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
    
    public boolean isLockedOut() {
        if (lockoutUntil == null) {
            return false;
        }
        boolean locked = LocalDateTime.now().isBefore(lockoutUntil);
        if (!locked && isLockedOut) {
            this.isLockedOut = false;
        }
        return locked;
    }
    
    public void setIsLockedOut(boolean isLockedOut) {
        this.isLockedOut = isLockedOut;
    }
    
    public void reset() {
        this.failedAttempts = 0;
        this.lockoutUntil = null;
        this.isLockedOut = false;
        this.updatedAt = LocalDateTime.now();
    }
}

