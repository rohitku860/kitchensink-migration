package com.kitchensink.service;

import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    
    public UserService(UserRepository userRepository, EncryptionService encryptionService,
                      InputSanitizationService sanitizationService, RoleService roleService,
                      UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.sanitizationService = sanitizationService;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
    }
    
    public User createUser(String name, String email, String isdCode, String phoneNumber, String roleName,
                          String dateOfBirth, String address, String city, String country) {
        logger.debug("Creating user with email: [REDACTED], role: {}", roleName);
        
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
            // Audit logging handled by EntityListener
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
    
    @Cacheable(value = "userById", key = "#id")
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
        
        // Get admin role ID
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName("ADMIN");
        } catch (Exception e) {
            logger.warn("Admin role not found, returning all users");
            return getAllUsers(pageable);
        }
        
        // Get all admin user IDs
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        logger.debug("Found {} admin users to exclude", adminUserIds.size());
        
        if (adminUserIds.isEmpty()) {
            // No admins to exclude, return all users
            return getAllUsers(pageable);
        }
        
        // Query users excluding admin IDs directly in the database
        Page<User> page = userRepository.findByIdNotInOrderByNameAsc(adminUserIds, pageable);
        
        // Decrypt PII for all users
        page.getContent().forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        logger.info("Retrieved {} users (excluding admins) out of {} total non-admin users", 
                page.getNumberOfElements(), page.getTotalElements());
        return page;
    }
    
    /**
     * Get all users excluding admin users using cursor-based pagination
     * @param cursor Last user ID from previous page (null for first page). Format: "userId" for ID sort, "userId:userName" for name sort
     * @param size Number of records to return
     * @param direction "next" or "previous"
     * @param sortBy "id" or "name"
     */
    public com.kitchensink.dto.CursorPageResponse<User> getAllUsersExcludingAdminsCursor(
            String cursor, int size, String direction, String sortBy) {
        logger.debug("Fetching users (excluding admins) with cursor: {}, size: {}, direction: {}, sortBy: {}", 
                cursor, size, direction, sortBy);
        
        // Get admin role ID
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName("ADMIN");
        } catch (Exception e) {
            logger.warn("Admin role not found, returning empty result");
            return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                    null, null, false, false, 0);
        }
        
        // Get all admin user IDs
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        logger.debug("Found {} admin users to exclude", adminUserIds.size());
        
        if (adminUserIds.isEmpty()) {
            logger.warn("No admin users found, returning empty result");
            return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                    null, null, false, false, 0);
        }
        
        // Default values
        if (size <= 0 || size > 100) {
            size = 10; // Default size, max 100
        }
        if (direction == null || direction.isEmpty()) {
            direction = "next";
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "id"; // Default to ID for cursor-based pagination (more reliable)
        }
        
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size + 1); // Fetch one extra to check hasNext
        List<User> users;
        boolean hasNext = false;
        boolean hasPrevious = false;
        String nextCursor = null;
        String previousCursor = null;
        String cursorId = null;
        
        // Parse cursor if provided
        if (cursor != null && !cursor.isEmpty()) {
            if (cursor.contains(":")) {
                String[] parts = cursor.split(":", 2);
                cursorId = parts[0];
            } else {
                cursorId = cursor;
            }
        }
        
        if ("previous".equalsIgnoreCase(direction)) {
            // Fetching previous page (going backwards)
            if (cursorId == null || cursorId.isEmpty()) {
                return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                        null, null, false, false, 0);
            }
            
            // For previous, we use ID-based cursor (more reliable)
            users = userRepository.findByIdNotInAndIdLessThanOrderByIdDesc(adminUserIds, cursorId, pageable);
            
            // Reverse the list for previous page
            java.util.Collections.reverse(users);
            
            if (users.size() > size) {
                users = users.subList(0, size);
                hasPrevious = true;
            }
            
            if (!users.isEmpty()) {
                User firstUser = users.get(0);
                previousCursor = firstUser.getId();
                // Check if there are more records before
                hasPrevious = userRepository.existsByIdNotInAndIdLessThan(adminUserIds, previousCursor);
            }
            
            if (cursorId != null) {
                hasNext = true; // We came from a next page, so there's a next page
                nextCursor = cursorId;
            }
        } else {
            // Fetching next page (going forward)
            if (cursorId == null || cursorId.isEmpty()) {
                // First page - get first records sorted by ID
                Page<User> firstPage = userRepository.findByIdNotInOrderByNameAsc(adminUserIds, 
                        org.springframework.data.domain.PageRequest.of(0, size + 1));
                users = firstPage.getContent();
            } else {
                // Use ID-based cursor for reliability (works regardless of sortBy)
                users = userRepository.findByIdNotInAndIdGreaterThanOrderByIdAsc(adminUserIds, cursorId, pageable);
            }
            
            // Apply name sorting if requested (after fetching by ID)
            if ("name".equalsIgnoreCase(sortBy) && !users.isEmpty()) {
                users = users.stream()
                        .sorted((u1, u2) -> {
                            if (u1.getName() == null && u2.getName() == null) return 0;
                            if (u1.getName() == null) return 1;
                            if (u2.getName() == null) return -1;
                            return u1.getName().compareToIgnoreCase(u2.getName());
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (users.size() > size) {
                users = users.subList(0, size);
                hasNext = true;
            }
            
            if (!users.isEmpty()) {
                User lastUser = users.get(users.size() - 1);
                nextCursor = lastUser.getId();
                // Check if there are more records after
                hasNext = userRepository.existsByIdNotInAndIdGreaterThan(adminUserIds, nextCursor);
            }
            
            if (cursorId != null) {
                hasPrevious = true; // We came from a previous page, so there's a previous page
                previousCursor = cursorId;
            }
        }
        
        // Decrypt PII for all users
        users.forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        logger.info("Retrieved {} users (excluding admins) with cursor pagination", users.size());
        return new com.kitchensink.dto.CursorPageResponse<>(users, nextCursor, previousCursor, 
                hasNext, hasPrevious, users.size());
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
        
        // Get admin role ID
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName("ADMIN");
        } catch (Exception e) {
            logger.warn("Admin role not found, returning all search results");
            return searchUsersByName(name);
        }
        
        // Get all admin user IDs
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        logger.debug("Found {} admin users to exclude", adminUserIds.size());
        
        if (adminUserIds.isEmpty()) {
            return searchUsersByName(name);
        }
        
        // Search and filter out admin users
        List<User> results = searchUsersByName(name);
        List<User> filteredResults = results.stream()
                .filter(user -> !adminUserIds.contains(user.getId()))
                .collect(java.util.stream.Collectors.toList());
        
        logger.info("Found {} users (excluding admins) matching name: {}", filteredResults.size(), name);
        return filteredResults;
    }
    
    @CacheEvict(value = {"userById", "roleNameByUserId"}, key = "#id")
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
        
        // Create a copy of the old user state for audit logging
        User oldUser = new User();
        oldUser.setId(existingUser.getId());
        oldUser.setName(existingUser.getName());
        oldUser.setEmailHash(existingUser.getEmailHash());
        oldUser.setPhoneNumberHash(existingUser.getPhoneNumberHash());
        oldUser.setIsdCode(existingUser.getIsdCode());
        oldUser.setDateOfBirth(existingUser.getDateOfBirth());
        oldUser.setAddress(existingUser.getAddress());
        oldUser.setCity(existingUser.getCity());
        oldUser.setCountry(existingUser.getCountry());
        oldUser.setStatus(existingUser.getStatus());
        
        // Set old state for event listener
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
        
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
            User updated = userRepository.save(existingUser);
            logger.info("User updated successfully with ID: {}", id);
            // Audit logging handled by EntityListener
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate key violation during user update: {}", e.getMessage());
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
    public User updateUser(String id, String name, String email, String phoneNumber) {
        return updateUser(id, name, email, null, phoneNumber, null, null, null, null);
    }
    
    @CacheEvict(value = "userById", key = "#id")
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
        
        // Set old state for event listener
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
        
        phoneNumber = sanitizationService.sanitizeForPhone(phoneNumber);
        
        user.setPhoneNumber(phoneNumber);
        user.setPhoneNumberEncrypted(encryptionService.encrypt(phoneNumber));
        user.setPhoneNumberHash(encryptionService.hash(phoneNumber));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Phone number updated successfully for user ID: {}", id);
            // Audit logging handled by EntityListener
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate phone number: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Phone number already exists", "phoneNumber");
        }
    }
    
    @CacheEvict(value = "userById", key = "#id")
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
        
        // Set old state for event listener
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
        
        newEmail = sanitizationService.sanitizeForEmail(newEmail);
        
        user.setEmail(newEmail);
        user.setEmailEncrypted(encryptionService.encrypt(newEmail));
        user.setEmailHash(encryptionService.hash(newEmail));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Email updated successfully for user ID: {}", id);
            // Audit logging handled by EntityListener
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
    @CacheEvict(value = {"userById", "roleNameByUserId"}, key = "#id")
    public void deleteUser(String id) {
        logger.debug("Deleting user with ID: {}", id);
        getUserById(id); // Verify user exists
        userRepository.deleteById(id);
        userRoleService.deactivateUserRole(id);
        logger.info("User deleted successfully with ID: {}", id);
        // Audit logging handled by EntityListener (if needed, can add AfterDeleteEvent handling)
    }
}

