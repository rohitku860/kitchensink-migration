package com.kitchensink.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Document(collection = "user_roles")
public class UserRole {
    
    @Id
    private String id;
    
    @NotBlank(message = "User ID is required")
    @Indexed(unique = true)
    private String userId; // Reference to User collection
    
    @NotBlank(message = "Role ID is required")
    private String roleId; // Reference to Role collection
    
    private LocalDateTime assignedAt;
    
    private LocalDateTime updatedAt;
    
    private boolean active = true;
    
    // Constructors
    public UserRole() {
        this.assignedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }
    
    public UserRole(String userId, String roleId) {
        this();
        this.userId = userId;
        this.roleId = roleId;
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
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}

