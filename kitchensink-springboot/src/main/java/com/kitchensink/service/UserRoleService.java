package com.kitchensink.service;

import com.kitchensink.model.UserRole;
import com.kitchensink.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserRoleService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRoleService.class);
    private final UserRoleRepository userRoleRepository;
    
    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }
    
    /**
     * Assign a role to a user. If a role already exists, it will be updated.
     */
    @Transactional
    public UserRole assignRoleToUser(String userId, String roleId) {
        logger.debug("Assigning role {} to user {}", roleId, userId);
        
        Optional<UserRole> existing = userRoleRepository.findByUserId(userId);
        
        if (existing.isPresent()) {
            // Update existing role assignment
            UserRole userRole = existing.get();
            userRole.setRoleId(roleId);
            userRole.setUpdatedAt(LocalDateTime.now());
            userRole.setActive(true);
            UserRole updated = userRoleRepository.save(userRole);
            logger.info("Role updated for user {}: {}", userId, roleId);
            return updated;
        } else {
            // Create new role assignment
            UserRole userRole = new UserRole(userId, roleId);
            UserRole saved = userRoleRepository.save(userRole);
            logger.info("Role assigned to user {}: {}", userId, roleId);
            return saved;
        }
    }
    
    /**
     * Get the role ID for a user
     */
    public Optional<String> getRoleIdByUserId(String userId) {
        logger.debug("Getting role for user {}", userId);
        Optional<UserRole> userRole = userRoleRepository.findByUserIdAndActiveTrue(userId);
        return userRole.map(UserRole::getRoleId);
    }
    
    /**
     * Get the UserRole mapping for a user
     */
    public Optional<UserRole> getUserRoleByUserId(String userId) {
        logger.debug("Getting UserRole mapping for user {}", userId);
        return userRoleRepository.findByUserIdAndActiveTrue(userId);
    }
    
    /**
     * Check if a user has a role assignment
     */
    public boolean hasRoleAssignment(String userId) {
        return userRoleRepository.existsByUserId(userId);
    }
    
    /**
     * Deactivate a user's role (soft delete)
     */
    @Transactional
    public void deactivateUserRole(String userId) {
        logger.debug("Deactivating role for user {}", userId);
        Optional<UserRole> userRole = userRoleRepository.findByUserId(userId);
        if (userRole.isPresent()) {
            UserRole ur = userRole.get();
            ur.setActive(false);
            ur.setUpdatedAt(LocalDateTime.now());
            userRoleRepository.save(ur);
            logger.info("Role deactivated for user {}", userId);
        }
    }
    
    /**
     * Get all user IDs that have a specific role
     */
    public java.util.List<String> getAllUserIdsByRoleId(String roleId) {
        logger.debug("Getting all user IDs for role {}", roleId);
        java.util.List<UserRole> userRoles = userRoleRepository.findByRoleIdAndActiveTrue(roleId);
        return userRoles.stream()
                .map(UserRole::getUserId)
                .collect(java.util.stream.Collectors.toList());
    }
}

