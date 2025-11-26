package com.kitchensink.dto;

public class LoginResponseDTO {
    
    private String token;
    private String userId;
    private String role;
    private String roleId;
    private String email;
    private String name;
    
    public LoginResponseDTO() {
    }
    
    public LoginResponseDTO(String token, String userId, String role, String roleId, String email, String name) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.roleId = roleId;
        this.email = email;
        this.name = name;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}

