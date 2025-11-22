package com.kitchensink.dto;

public class MemberResponseDTO {
    
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String restUrl; // REST URL for this member
    
    public MemberResponseDTO() {
    }
    
    public MemberResponseDTO(String id, String name, String email, String phoneNumber, String restUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.restUrl = restUrl;
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
    
    public String getRestUrl() {
        return restUrl;
    }
    
    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }
}

