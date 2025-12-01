package com.kitchensink.listener;

import com.kitchensink.model.User;

/**
 * Lightweight snapshot of User state for audit logging.
 * Contains only the fields needed to track changes.
 */
public class UserSnapshot {
    private String id;
    private String name;
    private String emailHash;
    private String phoneNumberHash;
    private String isdCode;
    private String dateOfBirth;
    private String address;
    private String city;
    private String country;
    private String status;
    
    public UserSnapshot() {
    }
    
    public static UserSnapshot from(User user) {
        UserSnapshot snapshot = new UserSnapshot();
        snapshot.id = user.getId();
        snapshot.name = user.getName();
        snapshot.emailHash = user.getEmailHash();
        snapshot.phoneNumberHash = user.getPhoneNumberHash();
        snapshot.isdCode = user.getIsdCode();
        snapshot.dateOfBirth = user.getDateOfBirth();
        snapshot.address = user.getAddress();
        snapshot.city = user.getCity();
        snapshot.country = user.getCountry();
        snapshot.status = user.getStatus();
        return snapshot;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

