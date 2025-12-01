package com.kitchensink.service;

import com.kitchensink.dto.UserCacheDTO;
import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.model.UserRoleType;
import com.kitchensink.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;
    
    public UserService(UserRepository userRepository, EncryptionService encryptionService,
                      InputSanitizationService sanitizationService, RoleService roleService,
                      UserRoleService userRoleService, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.sanitizationService = sanitizationService;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
        this.cacheManager = cacheManager;
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
    
    public User getUserById(String id) {
        UserCacheDTO dto = getUserCacheDTO(id);
        return convertToUser(dto);
    }
    
    public UserCacheDTO getUserCacheDTOById(String id) {
        return getUserCacheDTO(id);
    }
    
    @Cacheable(value = "userCache", key = "'user:' + #id")
    private UserCacheDTO getUserCacheDTO(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", id);
                    return new com.kitchensink.exception.ResourceNotFoundException("User", id);
                });
        
        return convertToCacheDTO(user);
    }
    
    private UserCacheDTO convertToCacheDTO(User user) {
        UserCacheDTO dto = new UserCacheDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setIsdCode(user.getIsdCode());
        dto.setEmailHash(user.getEmailHash());
        dto.setPhoneNumberHash(user.getPhoneNumberHash());
        dto.setEmailEncrypted(user.getEmailEncrypted());
        dto.setPhoneNumberEncrypted(user.getPhoneNumberEncrypted());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setCountry(user.getCountry());
        dto.setRegistrationDate(user.getRegistrationDate());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setStatus(user.getStatus());
        return dto;
    }
    
    private User convertToUser(UserCacheDTO dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setIsdCode(dto.getIsdCode());
        user.setEmailHash(dto.getEmailHash());
        user.setPhoneNumberHash(dto.getPhoneNumberHash());
        user.setEmailEncrypted(dto.getEmailEncrypted());
        user.setPhoneNumberEncrypted(dto.getPhoneNumberEncrypted());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setAddress(dto.getAddress());
        user.setCity(dto.getCity());
        user.setCountry(dto.getCountry());
        user.setRegistrationDate(dto.getRegistrationDate());
        user.setLastLoginDate(dto.getLastLoginDate());
        user.setStatus(dto.getStatus());
        
        if (user.getEmailEncrypted() != null) {
            user.setEmail(encryptionService.decrypt(user.getEmailEncrypted()));
        }
        if (user.getPhoneNumberEncrypted() != null) {
            user.setPhoneNumber(encryptionService.decrypt(user.getPhoneNumberEncrypted()));
        }
        
        return user;
    }
    
    public com.kitchensink.dto.CursorPageResponse<User> getAllUsersExcludingAdminsCursor(
            String cursor, int size, com.kitchensink.enums.Direction direction) {
        Role adminRole;
        try {
            adminRole = roleService.getRoleByName(UserRoleType.ADMIN.getName());
        } catch (Exception e) {
            logger.warn("Admin role not found, returning empty result");
            return new com.kitchensink.dto.CursorPageResponse<>(java.util.Collections.emptyList(), 
                    null, null, false, false, 0, 0, 0, 0, null, null);
        }
        
        List<String> adminUserIds = userRoleService.getAllUserIdsByRoleId(adminRole.getId());
        logger.debug("Found {} admin user IDs to exclude", adminUserIds.size());
        
        if (size <= 0 || size > 100) {
            size = 10;
        }
        if (direction == null) {
            direction = com.kitchensink.enums.Direction.NEXT;
        }
        
        String cursorId = cursor;
        List<User> users;
        String nextScrollId = null;
        String prevScrollId = null;
        boolean hasNext = false;
        boolean hasPrevious = false;
        
        Page<User> firstPage = userRepository.findByIdNotInOrderByNameAsc(adminUserIds, 
                org.springframework.data.domain.PageRequest.of(0, size));
        long totalElements = firstPage.getTotalElements();
        int totalPages = firstPage.getTotalPages();
        
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size + 1);
        
            if (cursorId == null || cursorId.isEmpty()) {
            logger.debug("Fetching first page - adminUserIds: {}, size: {}", adminUserIds.size(), size);
            List<User> fetchedUsers = firstPage.getContent();
            logger.debug("Query returned {} users from database", fetchedUsers.size());
            
            if (!fetchedUsers.isEmpty()) {
                prevScrollId = fetchedUsers.get(0).getId();
                nextScrollId = fetchedUsers.get(fetchedUsers.size() - 1).getId();
            }
            
            hasNext = firstPage.hasNext();
            hasPrevious = false;
            users = sortUsersByName(fetchedUsers);
        } else {
            if (direction == com.kitchensink.enums.Direction.PREV) {
                logger.debug("Fetching previous page - cursorId: {}, adminUserIds: {}", cursorId, adminUserIds.size());
                List<User> fetchedUsers = userRepository.findByIdNotInAndIdLessThanOrderByIdAsc(adminUserIds, cursorId, pageable);
                logger.debug("Query returned {} users from database", fetchedUsers.size());
                
                hasNext = true;
                
                if (!fetchedUsers.isEmpty()) {
                    if (fetchedUsers.size() > size) {
                        hasPrevious = true;
                        prevScrollId = fetchedUsers.get(fetchedUsers.size() - size).getId();
                        fetchedUsers = fetchedUsers.subList(fetchedUsers.size() - size, fetchedUsers.size());
                    } else {
                        prevScrollId = fetchedUsers.get(0).getId();
                        hasPrevious = fetchedUsers.size() == size + 1;
                    }
                    nextScrollId = fetchedUsers.get(fetchedUsers.size() - 1).getId();
                } else {
                    hasPrevious = false;
                }
                
                users = sortUsersByName(fetchedUsers);
            } else {
                logger.debug("Fetching next page - cursorId: {}, adminUserIds: {}", cursorId, adminUserIds.size());
                List<User> fetchedUsers = userRepository.findByIdNotInAndIdGreaterThanOrderByIdAsc(adminUserIds, cursorId, pageable);
                logger.debug("Query returned {} users from database", fetchedUsers.size());
                
                hasPrevious = true;
                
                if (!fetchedUsers.isEmpty()) {
                    prevScrollId = fetchedUsers.get(0).getId();
                    if (fetchedUsers.size() > size) {
                        hasNext = true;
                        nextScrollId = fetchedUsers.get(size - 1).getId();
                        fetchedUsers = fetchedUsers.subList(0, size);
                    } else {
                        nextScrollId = fetchedUsers.get(fetchedUsers.size() - 1).getId();
                        hasNext = false;
                    }
                } else {
                    hasNext = false;
                }
                
                users = sortUsersByName(fetchedUsers);
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
        
        int currentPage = cursorId == null || cursorId.isEmpty() ? 0 : 1;
        
        logger.debug("Returning {} users, totalElements: {}, totalPages: {}, currentPage: {}, nextScrollId: {}, prevScrollId: {}", 
                users.size(), totalElements, totalPages, currentPage, nextScrollId, prevScrollId);
        
        return new com.kitchensink.dto.CursorPageResponse<>(users, null, null, 
                hasNext, hasPrevious, users.size(), totalElements, totalPages, currentPage, nextScrollId, prevScrollId);
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
    
    private List<User> sortUsersByName(List<User> users) {
        if (users == null || users.isEmpty()) {
            return users;
        }
        
        return users.stream()
                .sorted((u1, u2) -> {
                    String name1 = u1.getName() != null ? u1.getName() : "";
                    String name2 = u2.getName() != null ? u2.getName() : "";
                    int comparison = name1.compareToIgnoreCase(name2);
                    if (comparison == 0) {
                        String id1 = u1.getId() != null ? u1.getId() : "";
                        String id2 = u2.getId() != null ? u2.getId() : "";
                        return id1.compareTo(id2);
                    }
                    return comparison;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    @CacheEvict(value = "userCache", key = "'user:' + #id")
    public User updateUser(String id, String name, String email, String isdCode, String phoneNumber,
                          String dateOfBirth, String address, String city, String country) {
        User existingUser = getUserByIdDirect(id);
        
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(
            com.kitchensink.listener.UserSnapshot.from(existingUser));
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
    
    @CacheEvict(value = "userCache", key = "'user:' + #id")
    public User updateUserPhoneNumber(String id, String phoneNumber) {
        User user = getUserByIdDirect(id);
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(
            com.kitchensink.listener.UserSnapshot.from(user));
        
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
    
    @CacheEvict(value = "userCache", key = "'user:' + #id")
    public User updateUserEmail(String id, String newEmail) {
        User user = getUserByIdDirect(id);
        com.kitchensink.listener.UserMongoEventListener.setOldUserState(
            com.kitchensink.listener.UserSnapshot.from(user));
        
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
    
    @CacheEvict(value = "userCache", key = "'user:' + #id")
    public void deleteUser(String id) {
        getUserByIdDirect(id);
        userRepository.deleteById(id);
        userRoleService.deactivateUserRole(id);
        
        org.springframework.cache.Cache cache = cacheManager.getCache("userCache");
        if (cache != null) {
            cache.evict("roleName:" + id);
        }
        
        logger.info("User deleted successfully with ID: {}", id);
    }
    
    private User getUserByIdDirect(String id) {
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
}

