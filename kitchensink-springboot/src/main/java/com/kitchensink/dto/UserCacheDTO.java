package com.kitchensink.dto;

import java.time.LocalDateTime;

public class UserCacheDTO {
    
    private String id;
    private String name;
    private String isdCode;
    private String emailHash;
    private String phoneNumberHash;
    private String emailEncrypted;
    private String phoneNumberEncrypted;
    private String dateOfBirth;
    private String address;
    private String city;
    private String country;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private String status;
    
    public UserCacheDTO() {
    }
    
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
    
    public String getIsdCode() {
        return isdCode;
    }
    
    public void setIsdCode(String isdCode) {
        this.isdCode = isdCode;
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
}

