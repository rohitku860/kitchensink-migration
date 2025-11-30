package com.kitchensink.service;

import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.model.UserRoleType;
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
        name = sanitizationService.sanitizeForName(name);
        email = sanitizationService.sanitizeForEmail(email);
        phoneNumber = sanitizationService.sanitizeForPhone(phoneNumber);
        if (isdCode != null) {
            isdCode = isdCode.trim();
        }
        
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
            userRoleService.assignRoleToUser(saved.getId(), role.getId());
            logger.info("User created successfully with ID: {}", saved.getId());
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
    
    public User createUser(String name, String email, String phoneNumber, String roleName) {
        return createUser(name, email, null, phoneNumber, roleName, null, null, null, null);
    }
    
    public Optional<User> getUserByEmail(String email) {
        String emailHash = encryptionService.hash(email);
        Optional<User> userOpt = userRepository.findByEmailHash(emailHash);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
        }
        
        return userOpt;
    }
    
    @Cacheable(value = "userById", key = "#id")
    public User getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new com.kitchensink.exception.ResourceNotFoundException("User", id);
                });
        
        if (user.getEmailEncrypted() != null) {
            user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
        }
        if (user.getPhoneNumberEncrypted() != null) {
            user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
        }
        
        return user;
    }
    
    public Page<User> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAllByOrderByNameAsc(pageable);
        
        page.getContent().forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        return page;
    }
    
    public Page<User> getAllUsersExcludingAdmins(Pageable pageable) {
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName(UserRoleType.ADMIN.getName());
        } catch (Exception e) {
            logger.warn("Admin role not found, returning all users");
            return getAllUsers(pageable);
        }
        
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        
        if (adminUserIds.isEmpty()) {
            return getAllUsers(pageable);
        }
        
        Page<User> page = userRepository.findByIdNotInOrderByNameAsc(adminUserIds, pageable);
        
        page.getContent().forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        return page;
    }
    
    public com.kitchensink.dto.CursorPageResponse<User> getAllUsersExcludingAdminsCursor(
            String cursor, int size, String direction, String sortBy) {
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName(UserRoleType.ADMIN.getName());
        } catch (Exception e) {
            logger.warn("Admin role not found, returning empty result");
            return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                    null, null, false, false, 0);
        }
        
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        
        if (adminUserIds.isEmpty()) {
            logger.warn("No admin users found, returning empty result");
            return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                    null, null, false, false, 0);
        }
        
        if (size <= 0 || size > 100) {
            size = 10;
        }
        if (direction == null || direction.isEmpty()) {
            direction = "next";
        }
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "id";
        }
        
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size + 1);
        List<User> users;
        boolean hasNext = false;
        boolean hasPrevious = false;
        String nextCursor = null;
        String previousCursor = null;
        String cursorId = null;
        
        if (cursor != null && !cursor.isEmpty()) {
            if (cursor.contains(":")) {
                String[] parts = cursor.split(":", 2);
                cursorId = parts[0];
            } else {
                cursorId = cursor;
            }
        }
        
        if ("previous".equalsIgnoreCase(direction)) {
            if (cursorId == null || cursorId.isEmpty()) {
                return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                        null, null, false, false, 0);
            }
            
            users = userRepository.findByIdNotInAndIdLessThanOrderByIdAsc(adminUserIds, cursorId, pageable);
            
            if (users.size() > size) {
                users = users.subList(0, size);
                hasPrevious = true;
            }
            
            if (!users.isEmpty()) {
                User firstUser = users.get(0);
                previousCursor = firstUser.getId();
                hasPrevious = userRepository.existsByIdNotInAndIdLessThan(adminUserIds, previousCursor);
            }
            
            if (cursorId != null) {
                hasNext = true;
                nextCursor = cursorId;
            }
        } else {
            if (cursorId == null || cursorId.isEmpty()) {
                Page<User> firstPage = userRepository.findByIdNotInOrderByNameAsc(adminUserIds, 
                        org.springframework.data.domain.PageRequest.of(0, size + 1));
                users = firstPage.getContent();
            } else {
                users = userRepository.findByIdNotInAndIdGreaterThanOrderByIdAsc(adminUserIds, cursorId, pageable);
            }
            
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
                hasNext = userRepository.existsByIdNotInAndIdGreaterThan(adminUserIds, nextCursor);
            }
            
            if (cursorId != null) {
                hasPrevious = true;
                previousCursor = cursorId;
            }
        }
        
        users.forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        return new com.kitchensink.dto.CursorPageResponse<>(users, nextCursor, previousCursor, 
                hasNext, hasPrevious, users.size());
    }
    
    public List<User> searchUsersByName(String name) {
        String sanitizedName = sanitizationService.sanitizeForName(name);
        String pattern = ".*" + sanitizedName + ".*";
        List<User> results = userRepository.searchByName(pattern);
        
        results.forEach(user -> {
            if (user.getEmailEncrypted() != null) {
                user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
            }
            if (user.getPhoneNumberEncrypted() != null) {
                user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
            }
        });
        
        return results;
    }
    
    public List<User> searchUsersByNameExcludingAdmins(String name) {
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName(UserRoleType.ADMIN.getName());
        } catch (Exception e) {
            logger.warn("Admin role not found, returning all search results");
            return searchUsersByName(name);
        }
        
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        
        if (adminUserIds.isEmpty()) {
            return searchUsersByName(name);
        }
        
        List<User> results = searchUsersByName(name);
        List<User> filteredResults = results.stream()
                .filter(user -> !adminUserIds.contains(user.getId()))
                .collect(java.util.stream.Collectors.toList());
        
        return filteredResults;
    }
    
    @CacheEvict(value = {"userById", "roleNameByUserId"}, key = "#id")
    public User updateUser(String id, String name, String email, String isdCode, String phoneNumber,
                          String dateOfBirth, String address, String city, String country) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found for update with ID: {}", id);
                    return new com.kitchensink.exception.ResourceNotFoundException("User", id);
                });
        
        if (existingUser.getEmailEncrypted() != null) {
            existingUser.setEmail(encryptionService.decrypt(existingUser.getEmailEncrypted()));
        }
        if (existingUser.getPhoneNumberEncrypted() != null) {
            existingUser.setPhoneNumber(encryptionService.decrypt(existingUser.getPhoneNumberEncrypted()));
        }
        
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
        
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
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
        User user = getUserById(id);
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
        
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
        
        phoneNumber = sanitizationService.sanitizeForPhone(phoneNumber);
        
        user.setPhoneNumber(phoneNumber);
        user.setPhoneNumberEncrypted(encryptionService.encrypt(phoneNumber));
        user.setPhoneNumberHash(encryptionService.hash(phoneNumber));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Phone number updated successfully for user ID: {}", id);
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate phone number: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Phone number already exists", "phoneNumber");
        }
    }
    
    @CacheEvict(value = "userById", key = "#id")
    public User updateUserEmail(String id, String newEmail) {
        User user = getUserById(id);
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
        
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(oldUser);
        
        newEmail = sanitizationService.sanitizeForEmail(newEmail);
        
        user.setEmail(newEmail);
        user.setEmailEncrypted(encryptionService.encrypt(newEmail));
        user.setEmailHash(encryptionService.hash(newEmail));
        
        try {
            User updated = userRepository.save(user);
            logger.info("Email updated successfully for user ID: {}", id);
            return updated;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.warn("Duplicate email: {}", e.getMessage());
            throw new com.kitchensink.exception.ResourceConflictException("Email already exists", "email");
        }
    }
    
    public void updateLastLoginDate(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.kitchensink.exception.ResourceNotFoundException("User", userId));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }
    
    public boolean emailExists(String email) {
        String emailHash = encryptionService.hash(email);
        return userRepository.existsByEmailHash(emailHash);
    }
    
    public boolean phoneNumberExists(String phoneNumber) {
        String phoneNumberHash = encryptionService.hash(phoneNumber);
        return userRepository.existsByPhoneNumberHash(phoneNumberHash);
    }
    
    @CacheEvict(value = {"userById", "roleNameByUserId"}, key = "#id")
    public void deleteUser(String id) {
        getUserById(id);
        userRepository.deleteById(id);
        userRoleService.deactivateUserRole(id);
        logger.info("User deleted successfully with ID: {}", id);
    }
}

