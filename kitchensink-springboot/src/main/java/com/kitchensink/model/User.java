package com.kitchensink.model;

import com.kitchensink.validation.AlphaOnly;
import com.kitchensink.validation.IndianIsdCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @AlphaOnly(message = "Name must contain only letters and spaces")
    private String name;
    
    // PII Fields - Only encrypted values stored in database
    @Transient
    private String email;  // Only used temporarily, not persisted
    
    @Transient
    private String phoneNumber;  // Only used temporarily, not persisted
    
    @IndianIsdCode(message = "ISD code must be +91 for Indian numbers")
    private String isdCode; // Country code (e.g., +91)
    
    // Hash fields - For indexing and uniqueness checks
    private String emailHash;
    private String phoneNumberHash;
    
    // Encrypted PII - These are the only fields stored in database
    private String emailEncrypted;
    private String phoneNumberEncrypted;
    
    // Additional user fields
    private String dateOfBirth; // Format: YYYY-MM-DD
    
    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;
    
    @Size(max = 50, message = "City must not exceed 50 characters")
    @AlphaOnly(message = "City must contain only letters and spaces")
    private String city;
    
    @Size(max = 50, message = "Country must not exceed 50 characters")
    @AlphaOnly(message = "Country must contain only letters and spaces")
    private String country;
    
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    
    private String status = "ACTIVE";
    
    // Constructors
    public User() {
        this.registrationDate = LocalDateTime.now();
        this.status = "ACTIVE";
    }
    
    // Constructor removed - use setters instead
    
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
    
    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
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
    
    public String getIsdCode() {
        return isdCode;
    }
    
    public void setIsdCode(String isdCode) {
        this.isdCode = isdCode;
    }
    
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    // Note: Role information is stored in user_roles collection, not in User model
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='[ENCRYPTED]'" +
                ", phoneNumber='[ENCRYPTED]'" +
                ", registrationDate=" + registrationDate +
                ", status='" + status + '\'' +
                '}';
    }
}

