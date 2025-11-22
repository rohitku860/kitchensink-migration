package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Document(collection = "members")
public class Member {
    
    @Id
    private String id;
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 25, message = "Name must be between 1 and 25 characters")
    @Pattern(regexp = "[^0-9]*", message = "Name must not contain numbers")
    private String name;
    
    // PII Fields - Only encrypted values stored in database
    // Plain text fields are @Transient (not persisted to MongoDB)
    @org.springframework.data.annotation.Transient
    private String email;  // Only used temporarily, not persisted
    
    @org.springframework.data.annotation.Transient
    private String phoneNumber;  // Only used temporarily, not persisted
    
    // Hash fields - For indexing and uniqueness checks (deterministic)
    // Same email/phone always produces same hash, allowing unique indexes
    private String emailHash;
    
    private String phoneNumberHash;
    
    // Encrypted PII - These are the only fields stored in database
    // Note: Validation happens at DTO level, not on encrypted strings
    private String emailEncrypted;
    
    private String phoneNumberEncrypted;
    
    private LocalDateTime registrationDate;
    
    private String status = "ACTIVE";
    
    // Constructors
    public Member() {
        this.registrationDate = LocalDateTime.now();
        this.status = "ACTIVE";
    }
    
    public Member(String name, String email, String phoneNumber) {
        this();
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getEmailEncrypted() {
        return emailEncrypted;
    }
    
    public void setEmailEncrypted(String emailEncrypted) {
        this.emailEncrypted = emailEncrypted;
    }
    
    public String getPhoneNumberEncrypted() {
        return phoneNumberEncrypted;
    }
    
    public void setPhoneNumberEncrypted(String phoneNumberEncrypted) {
        this.phoneNumberEncrypted = phoneNumberEncrypted;
    }
    
    public String getEmailHash() {
        return emailHash;
    }
    
    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }
    
    public String getPhoneNumberHash() {
        return phoneNumberHash;
    }
    
    public void setPhoneNumberHash(String phoneNumberHash) {
        this.phoneNumberHash = phoneNumberHash;
    }
    
    @Override
    public String toString() {
        return "Member{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='[ENCRYPTED]'" +  // Never expose PII in logs
                ", phoneNumber='[ENCRYPTED]'" +  // Never expose PII in logs
                ", registrationDate=" + registrationDate +
                ", status='" + status + '\'' +
                '}';
    }
}

