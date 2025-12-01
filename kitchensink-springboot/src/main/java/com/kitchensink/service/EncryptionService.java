package com.kitchensink.service;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
public class EncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private final StringEncryptor currentEncryptor;
    private final Map<String, StringEncryptor> legacyEncryptors;
    private final String currentKeyVersion;
    
    public EncryptionService(
            @Value("${app.encryption.password:default-secret-key-change-in-production}") String encryptionPassword,
            @Value("${app.encryption.key-version:1}") String keyVersion,
            @Value("${app.encryption.legacy-keys:}") String legacyKeys) {
        this.currentKeyVersion = keyVersion;
        this.currentEncryptor = createEncryptor(encryptionPassword);
        this.legacyEncryptors = loadLegacyEncryptors(legacyKeys);
        logger.info("EncryptionService initialized with key version: {}", keyVersion);
    }
    
    private Map<String, StringEncryptor> loadLegacyEncryptors(String legacyKeysConfig) {
        Map<String, StringEncryptor> legacy = new HashMap<>();
        if (legacyKeysConfig != null && !legacyKeysConfig.isEmpty()) {
          
        	String[] entries = legacyKeysConfig.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split(":", 2);
                if (parts.length == 2) {
                    String version = parts[0].trim();
                    String password = parts[1].trim();
                    legacy.put(version, createEncryptor(password));
                    logger.info("Loaded legacy encryption key for version: {}", version);
                }
            }
        }
        return legacy;
    }
    
    private StringEncryptor createEncryptor(String password) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
    
    /**
     * Encrypts data with current key version.
     * Format: "v{version}:{encryptedData}"
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            String encrypted = currentEncryptor.encrypt(plainText);
            // Prefix with key version for rotation support
            return "v" + currentKeyVersion + ":" + encrypted;
        } catch (Exception e) {
            logger.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypts data, automatically handling key versioning.
     * Tries current key first, then falls back to legacy keys if needed.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            // Check if encrypted text has version prefix
            if (encryptedText.startsWith("v") && encryptedText.contains(":")) {
                String[] parts = encryptedText.split(":", 2);
                if (parts.length == 2) {
                    String version = parts[0].substring(1); // Remove "v" prefix
                    String encryptedData = parts[1];
                    
                    // Try current key first
                    if (version.equals(currentKeyVersion)) {
                        return currentEncryptor.decrypt(encryptedData);
                    }
                    
                    // Try legacy keys
                    StringEncryptor legacyEncryptor = legacyEncryptors.get(version);
                    if (legacyEncryptor != null) {
                        logger.debug("Decrypting with legacy key version: {}", version);
                        String decrypted = legacyEncryptor.decrypt(encryptedData);
                        // Auto-re-encrypt with current key (lazy re-encryption)
                        logger.info("Auto re-encrypting data from version {} to {}", version, currentKeyVersion);
                        return decrypted; // Note: Caller should re-encrypt and save
                        // For automatic re-encryption, you'd need to update the entity here
                    } else {
                        logger.error("No encryptor found for key version: {}", version);
                        throw new RuntimeException("Cannot decrypt: unknown key version " + version);
                    }
                }
            }
            
            // Legacy format (no version prefix) - try current key
            logger.warn("Decrypting legacy format (no version prefix) - consider re-encrypting");
            return currentEncryptor.decrypt(encryptedText);
            
        } catch (Exception e) {
            logger.error("Error decrypting data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Generates SHA-256 hash for indexing and uniqueness checks.
     * Hash is deterministic - same input always produces same hash.
     * This allows us to create unique indexes and fast lookups.
     * 
     * @param plainText The plain text to hash
     * @return SHA-256 hash in hexadecimal format
     */
    public String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
