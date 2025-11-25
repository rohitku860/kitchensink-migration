package com.kitchensink.service;

import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final InputSanitizationService sanitizationService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final AuditService auditService;
    
    public UserService(UserRepository userRepository, EncryptionService encryptionService,
                      InputSanitizationService sanitizationService, RoleService roleService,
                      UserRoleService userRoleService, AuditService auditService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.sanitizationService = sanitizationService;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
        this.auditService = auditService;
    }
    
    public User createUser(String name, String email, String isdCode, String phoneNumber, String roleName,
                          String dateOfBirth, String address, String city, String country) {
        logger.debug("Creating user with email: [REDACTED], role: {}", roleName);
        
        // Check for duplicate email
        if (emailExists(email)) {
            throw new com.kitchensink.exception.ResourceConflictException("Email already exists", "email");
        }
        
        // Check for duplicate phone number (phone number alone, ISD code is separate)
        if (phoneNumberExists(phoneNumber)) {
            throw new com.kitchensink.exception.ResourceConflictException("Phone number already exists", "phoneNumber");
        }
        
        // Sanitize inputs
        name = sanitizationService.sanitizeForName(name);
        email = sanitizationService.sanitizeForEmail(email);
        phoneNumber = sanitizationService.sanitizeForPhone(phoneNumber);
        if (isdCode != null) {
            isdCode = isdCode.trim();
        }
        
        // Get or create role
        Role role = roleService.createRoleIfNotExists(roleName, roleName + " role");
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setIsdCode(isdCode);
        user.setPhoneNumber(phoneNumber);
        user.setDateOfBirth(dateOfBirth);
        user.setAddress(address);
        user.setCity(city);
        user.setCountry(country);
        
        // Encrypt and hash PII
        user.setEmailEncrypted(encryptionService.encrypt(email));
        user.setPhoneNumberEncrypted(encryptionService.encrypt(phoneNumber));
        user.setEmailHash(encryptionService.hash(email));
        user.setPhoneNumberHash(encryptionService.hash(phoneNumber));
        
        if (user.getRegistrationDate() == null) {
            user.setRegistrationDate(LocalDateTime.now());
        }
        
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("ACTIVE");
        }
        
        try {
            User saved = userRepository.save(user);
            // Assign role to user in separate collection
            userRoleService.assignRoleToUser(saved.getId(), role.getId());
            logger.info("User created successfully with ID: {}", saved.getId());
            // Log audit
            auditService.logUserCreated(saved);
            return saved;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate key violation during user creation: {}", e.getMessage());
            String message = e.getMessage();
            String field = "error";
            String errorMessage = "Duplicate key violation";
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("emailhash") || lowerMessage.contains("email_hash_unique_idx")) {
                    field = "email";
                    errorMessage = "Email already exists";
                } else if (lowerMessage.contains("phonenumberhash") || lowerMessage.contains("phonenumber_hash_unique_idx")) {
                    field = "phoneNumber";
                    errorMessage = "Phone number already exists";
                }
            }
            throw new com.kitchensink.exception.ResourceConflictException(errorMessage, field);
        }
    }
    
    // Overloaded method for backward compatibility
    public User createUser(String name, String email, String phoneNumber, String roleName) {
        return createUser(name, email, null, phoneNumber, roleName, null, null, null, null);
    }
    
    public Optional<User> getUserByEmail(String email) {
        logger.debug("Fetching user by email: [REDACTED]");
        String emailHash = encryptionService.hash(email);
        Optional<User> userOpt = userRepository.findByEmailHash(emailHash);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Decrypt PII for use
            user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
        }
        
        return userOpt;
    }
    
    public User getUserById(String id) {
        logger.debug("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new com.kitchensink.exception.ResourceNotFoundException("User", id);
                });
        
        // Decrypt PII for use
        if (user.getEmailEncrypted() != null) {
            user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
        }
        if (user.getPhoneNumberEncrypted() != null) {
            user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
        }
        
        return user;
    }
    
    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching users - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<User> page = userRepository.findAllByOrderByNameAsc(pageable);
        
        // Decrypt PII for all users
        page.getContent().forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        logger.info("Retrieved {} users out of {} total", page.getNumberOfElements(), page.getTotalElements());
        return page;
    }
    
    /**
     * Get all users excluding admin users (for admin dashboard)
     */
    public Page<User> getAllUsersExcludingAdmins(Pageable pageable) {
        logger.debug("Fetching users (excluding admins) - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<User> page = userRepository.findAllByOrderByNameAsc(pageable);
        
        // Filter out admin users
        List<User> filteredUsers = page.getContent().stream()
                .filter(user -> {
                    try {
                        String roleName = roleService.getRoleNameByUserId(user.getId());
                        return !"ADMIN".equals(roleName);
                    } catch (Exception e) {
                        logger.warn("Error checking role for user {}: {}", user.getId(), e.getMessage());
                        return false; // Exclude if we can't determine role
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Decrypt PII for filtered users
        filteredUsers.forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        // Create a new page with filtered content
        Page<User> filteredPage = new org.springframework.data.domain.PageImpl<>(
                filteredUsers,
                pageable,
                filteredUsers.size() // Approximate total, actual count would require separate query
        );
        
        logger.info("Retrieved {} users (excluding admins) out of {} total", filteredUsers.size(), page.getTotalElements());
        return filteredPage;
    }
    
    public List<User> searchUsersByName(String name) {
        logger.debug("Searching users by name: {}", name);
        String sanitizedName = sanitizationService.sanitizeForName(name);
        String pattern = ".*" + sanitizedName + ".*";
        List<User> results = userRepository.searchByName(pattern);
        
        // Decrypt PII for all users
        results.forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        logger.info("Found {} users matching name: {}", results.size(), name);
        return results;
    }
    
    /**
     * Search users by name excluding admin users (for admin dashboard)
     */
    public List<User> searchUsersByNameExcludingAdmins(String name) {
        logger.debug("Searching users (excluding admins) by name: {}", name);
        List<User> results = searchUsersByName(name);
        
        // Filter out admin users
        List<User> filteredResults = results.stream()
                .filter(user -> {
                    try {
                        String roleName = roleService.getRoleNameByUserId(user.getId());
                        return !"ADMIN".equals(roleName);
                    } catch (Exception e) {
                        logger.warn("Error checking role for user {}: {}", user.getId(), e.getMessage());
                        return false; // Exclude if we can't determine role
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        
        logger.info("Found {} users (excluding admins) matching name: {}", filteredResults.size(), name);
        return filteredResults;
    }
    
    public User updateUser(String id, String name, String email, String isdCode, String phoneNumber,
                          String dateOfBirth, String address, String city, String country) {
        logger.debug("Updating user with ID: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found for update with ID: {}", id);
                    return new com.kitchensink.exception.ResourceNotFoundException("User", id);
                });
        
        // Decrypt current values for comparison
        if (existingUser.getEmailEncrypted() != null) {
            existingUser.setEmail(encryptionService.decrypt(existingUser.getEmailEncrypted()));
        }
        if (existingUser.getPhoneNumberEncrypted() != null) {
            existingUser.setPhoneNumber(encryptionService.decrypt(existingUser.getPhoneNumberEncrypted()));
        }
        
        // Check for duplicate email if changing
        if (email != null && !email.equals(existingUser.getEmail())) {
            if (emailExists(email)) {
                throw new com.kitchensink.exception.ResourceConflictException("Email already exists", "email");
            }
        }
        
        // Check for duplicate phone if changing
        if (phoneNumber != null && !phoneNumber.equals(existingUser.getPhoneNumber())) {
            if (phoneNumberExists(phoneNumber)) {
                throw new com.kitchensink.exception.ResourceConflictException("Phone number already exists", "phoneNumber");
            }
        }
        
        // Sanitize and update inputs
        if (name != null) {
            existingUser.setName(sanitizationService.sanitizeForName(name));
        }
        
        if (email != null && !email.equals(existingUser.getEmail())) {
            existingUser.setEmail(email);
            existingUser.setEmailEncrypted(encryptionService.encrypt(email));
            existingUser.setEmailHash(encryptionService.hash(email));
        }
        
        if (isdCode != null) {
            existingUser.setIsdCode(isdCode.trim());
        }
        
        if (phoneNumber != null && !phoneNumber.equals(existingUser.getPhoneNumber())) {
            existingUser.setPhoneNumber(phoneNumber);
            existingUser.setPhoneNumberEncrypted(encryptionService.encrypt(phoneNumber));
            existingUser.setPhoneNumberHash(encryptionService.hash(phoneNumber));
        }
        
        if (dateOfBirth != null) {
            existingUser.setDateOfBirth(dateOfBirth);
        }
        
        if (address != null) {
            existingUser.setAddress(address);
        }
        
        if (city != null) {
            existingUser.setCity(city);
        }
        
        if (country != null) {
            existingUser.setCountry(country);
        }
        
        try {
            // Create a copy of the old user state for audit logging
            User oldUser = new User();
            oldUser.setId(existingUser.getId());
            oldUser.setName(existingUser.getName());
            oldUser.setEmail(existingUser.getEmail());
            oldUser.setEmailHash(existingUser.getEmailHash());
            oldUser.setPhoneNumber(existingUser.getPhoneNumber());
            oldUser.setPhoneNumberHash(existingUser.getPhoneNumberHash());
            oldUser.setIsdCode(existingUser.getIsdCode());
            oldUser.setDateOfBirth(existingUser.getDateOfBirth());
            oldUser.setAddress(existingUser.getAddress());
            oldUser.setCity(existingUser.getCity());
            oldUser.setCountry(existingUser.getCountry());
            oldUser.setStatus(existingUser.getStatus());
            
            User updated = userRepository.save(existingUser);
            logger.info("User updated successfully with ID: {}", id);
            // Log audit
            auditService.logUserUpdated(oldUser, updated);
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate key violation during user update: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Email or phone number already exists", "email");
        }
    }
    
    // Overloaded method for backward compatibility
    public User updateUser(String id, String name, String email, String phoneNumber) {
        return updateUser(id, name, email, null, phoneNumber, null, null, null, null);
    }
    
    public User updateUserPhoneNumber(String id, String phoneNumber) {
        logger.debug("Updating phone number for user ID: {}", id);
        
        User user = getUserById(id);
        // Create a copy of the old user state for audit logging
        User oldUser = new User();
        oldUser.setId(user.getId());
        oldUser.setName(user.getName());
        oldUser.setEmailHash(user.getEmailHash());
        oldUser.setPhoneNumberHash(user.getPhoneNumberHash());
        oldUser.setIsdCode(user.getIsdCode());
        oldUser.setDateOfBirth(user.getDateOfBirth());
        oldUser.setAddress(user.getAddress());
        oldUser.setCity(user.getCity());
        oldUser.setCountry(user.getCountry());
        oldUser.setStatus(user.getStatus());
        
        phoneNumber = sanitizationService.sanitizeForPhone(phoneNumber);
        
        user.setPhoneNumber(phoneNumber);
        user.setPhoneNumberEncrypted(encryptionService.encrypt(phoneNumber));
        user.setPhoneNumberHash(encryptionService.hash(phoneNumber));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Phone number updated successfully for user ID: {}", id);
            // Log audit
            auditService.logUserUpdated(oldUser, updated);
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate phone number: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Phone number already exists", "phoneNumber");
        }
    }
    
    public User updateUserEmail(String id, String newEmail) {
        logger.debug("Updating email for user ID: {}", id);
        
        User user = getUserById(id);
        // Create a copy of the old user state for audit logging
        User oldUser = new User();
        oldUser.setId(user.getId());
        oldUser.setName(user.getName());
        oldUser.setEmailHash(user.getEmailHash());
        oldUser.setPhoneNumberHash(user.getPhoneNumberHash());
        oldUser.setIsdCode(user.getIsdCode());
        oldUser.setDateOfBirth(user.getDateOfBirth());
        oldUser.setAddress(user.getAddress());
        oldUser.setCity(user.getCity());
        oldUser.setCountry(user.getCountry());
        oldUser.setStatus(user.getStatus());
        
        newEmail = sanitizationService.sanitizeForEmail(newEmail);
        
        user.setEmail(newEmail);
        user.setEmailEncrypted(encryptionService.encrypt(newEmail));
        user.setEmailHash(encryptionService.hash(newEmail));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Email updated successfully for user ID: {}", id);
            // Log audit
            auditService.logUserUpdated(oldUser, updated);
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate email: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Email already exists", "email");
        }
    }
    
    public void updateLastLoginDate(String userId) {
        logger.debug("Updating last login date for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("User", userId));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }
    
    public boolean emailExists(String email) {
        logger.debug("Checking if email exists: [REDACTED]");
        String emailHash = encryptionService.hash(email);
        return userRepository.existsByEmailHash(emailHash);
    }
    
    public boolean phoneNumberExists(String phoneNumber) {
        logger.debug("Checking if phone number exists: [REDACTED]");
        String phoneNumberHash = encryptionService.hash(phoneNumber);
        return userRepository.existsByPhoneNumberHash(phoneNumberHash);
    }
    
    /**
     * Delete a user by ID
     */
    public void deleteUser(String id) {
        logger.debug("Deleting user with ID: {}", id);
        // Get user before deletion for audit logging
        User user = getUserById(id);
        userRepository.deleteById(id);
        // Also deactivate user role mapping
        userRoleService.deactivateUserRole(id);
        logger.info("User deleted successfully with ID: {}", id);
        // Log audit
        auditService.logUserDeleted(user);
    }
}

