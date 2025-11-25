package com.kitchensink.config;

import com.kitchensink.model.Role;
import com.kitchensink.model.User;
import com.kitchensink.repository.UserRepository;
import com.kitchensink.service.EncryptionService;
import com.kitchensink.service.RoleService;
import com.kitchensink.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    
    public DataInitializer(UserRepository userRepository, EncryptionService encryptionService,
                          RoleService roleService, UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        Role adminRole = roleService.createRoleIfNotExists("ADMIN", "Administrator role with full access");
        roleService.createRoleIfNotExists("USER", "Regular user role");
        logger.info("Roles initialized");
        
        String adminEmail = "rohitku860@gmail.com";
        
        // Check if admin already exists
        String emailHash = encryptionService.hash(adminEmail);
        User admin;
        
        if (userRepository.existsByEmailHash(emailHash)) {
            logger.info("Admin user already exists, checking role mapping");
            // Get existing admin user
            admin = userRepository.findByEmailHash(emailHash)
                    .orElseThrow(() -> new RuntimeException("Admin user not found despite existence check"));
            
            // Check if role mapping exists, if not create it
            if (!userRoleService.hasRoleAssignment(admin.getId())) {
                logger.info("Admin user exists but no role mapping found, creating role mapping");
                userRoleService.assignRoleToUser(admin.getId(), adminRole.getId());
                logger.info("Role mapping created for existing admin user");
            } else {
                logger.info("Admin user and role mapping already exist");
            }
        } else {
            // Create default admin user
            admin = new User();
            admin.setName("Rohit");
            admin.setEmail(adminEmail);
            admin.setIsdCode("+91"); // Default ISD code
            admin.setPhoneNumber("1234567890"); // Dummy phone number
            admin.setStatus("ACTIVE");
            admin.setDateOfBirth(null);
            admin.setAddress("Dummy Address");
            admin.setCity("Dummy City");
            admin.setCountry("India");
            
            // Encrypt and hash PII
            admin.setEmailEncrypted(encryptionService.encrypt(adminEmail));
            admin.setPhoneNumberEncrypted(encryptionService.encrypt("1234567890"));
            admin.setEmailHash(emailHash);
            admin.setPhoneNumberHash(encryptionService.hash("1234567890"));
            
            User savedAdmin = userRepository.save(admin);
            // Assign role to user in separate collection
            userRoleService.assignRoleToUser(savedAdmin.getId(), adminRole.getId());
            logger.info("Default admin user created: {}", adminEmail);
        }
    }
}

