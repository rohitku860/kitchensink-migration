package com.kitchensink.service;

import com.kitchensink.model.Role;
import com.kitchensink.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final RoleRepository roleRepository;
    private final UserRoleService userRoleService;
    
    public RoleService(RoleRepository roleRepository, UserRoleService userRoleService) {
        this.roleRepository = roleRepository;
        this.userRoleService = userRoleService;
    }
    
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("Role", name));
    }
    
    public Role getRoleById(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("Role", id));
    }
    
    public Role createRoleIfNotExists(String name, String description) {
        Optional<Role> existing = roleRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        Role role = new Role(name, description);
        Role saved = roleRepository.save(role);
        logger.info("Role created: {}", name);
        return saved;
    }
    
    public boolean isAdmin(String roleId) {
        try {
            Role role = getRoleById(roleId);
            return "ADMIN".equals(role.getName());
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isUser(String roleId) {
        try {
            Role role = getRoleById(roleId);
            return "USER".equals(role.getName());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a user has ADMIN role (by userId)
     */
    public boolean isAdminByUserId(String userId) {
        try {
            Optional<String> roleIdOpt = userRoleService.getRoleIdByUserId(userId);
            if (roleIdOpt.isEmpty()) {
                return false;
            }
            return isAdmin(roleIdOpt.get());
        } catch (Exception e) {
            logger.warn("Error checking admin role for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a user has USER role (by userId)
     */
    public boolean isUserByUserId(String userId) {
        try {
            Optional<String> roleIdOpt = userRoleService.getRoleIdByUserId(userId);
            if (roleIdOpt.isEmpty()) {
                return false;
            }
            return isUser(roleIdOpt.get());
        } catch (Exception e) {
            logger.warn("Error checking user role for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get role name for a user (by userId)
     */
    public String getRoleNameByUserId(String userId) {
        Optional<String> roleIdOpt = userRoleService.getRoleIdByUserId(userId);
        if (roleIdOpt.isEmpty()) {
            throw new com.kitchensink.exception.ResourceNotFoundException("UserRole", userId);
        }
        Role role = getRoleById(roleIdOpt.get());
        return role.getName();
    }
    
    /**
     * Get role ID for a user (by userId)
     */
    public String getRoleIdByUserId(String userId) {
        return userRoleService.getRoleIdByUserId(userId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("UserRole", userId));
    }
}

