package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Document(collection = "otps")
public class Otp {
    
    @Id
    private String id;
    
    @NotBlank(message = "Email is required")
    private String emailHash; // Hash of email for lookup
    
    @Transient
    private String email; // Plain email (not persisted)
    
    @NotBlank(message = "OTP is required")
    private String otpCode; // Plain OTP (not persisted, only for email sending)
    
    @NotBlank(message = "OTP hash is required")
    private String otpHash; // Hash of OTP for comparison
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    private boolean used = false;
    
    private String purpose; // LOGIN, EMAIL_CHANGE, etc.
    
    // Constructors
    public Otp() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(10); // OTP expires in 10 minutes
        this.used = false;
    }
    
    public Otp(String email, String otpCode, String purpose) {
        this();
        this.email = email;
        this.otpCode = otpCode;
        this.purpose = purpose;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getEmailHash() {
        return emailHash;
    }
    
    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public String getOtpHash() {
        return otpHash;
    }
    
    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}

