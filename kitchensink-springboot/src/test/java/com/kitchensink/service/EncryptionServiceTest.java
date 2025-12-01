package com.kitchensink.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionService Unit Tests")
class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private String testPassword = "test-encryption-password-12345";
    private String testKeyVersion = "1";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(testPassword, testKeyVersion, "");
    }

    @Test
    @DisplayName("Should encrypt and decrypt text successfully")
    void testEncryptDecrypt_Success() {
        // Given
        String plainText = "test@example.com";

        // When
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encrypted).startsWith("v1:");
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should return null for null input")
    void testEncrypt_NullInput() {
        // When
        String result = encryptionService.encrypt(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return empty string for empty input")
    void testEncrypt_EmptyInput() {
        // When
        String result = encryptionService.encrypt("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should generate consistent hash for same input")
    void testHash_Consistency() {
        // Given
        String input = "test@example.com";

        // When
        String hash1 = encryptionService.hash(input);
        String hash2 = encryptionService.hash(input);

        // Then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 64 hex characters
        assertThat(hash1).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("Should generate different hashes for different inputs")
    void testHash_DifferentInputs() {
        // Given
        String input1 = "test1@example.com";
        String input2 = "test2@example.com";

        // When
        String hash1 = encryptionService.hash(input1);
        String hash2 = encryptionService.hash(input2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Should handle null input for hash")
    void testHash_NullInput() {
        // When
        String result = encryptionService.hash(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle empty input for hash")
    void testHash_EmptyInput() {
        // When
        String result = encryptionService.hash("");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should decrypt with legacy key")
    void testDecrypt_LegacyKey() {
        // Given
        EncryptionService oldService = new EncryptionService("old-password", "1", "");
        String encrypted = oldService.encrypt("test@example.com");

        EncryptionService newService = new EncryptionService("new-password", "2", "1:old-password");

        // When
        String decrypted = newService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should handle multiple legacy keys")
    void testMultipleLegacyKeys() {
        // Given
        EncryptionService service = new EncryptionService("current-password", "3", 
                "1:old-password-1,2:old-password-2");

        // When - decrypt with version 1
        EncryptionService v1Service = new EncryptionService("old-password-1", "1", "");
        String v1Encrypted = v1Service.encrypt("test@example.com");
        String decrypted = service.decrypt(v1Encrypted);

        // Then
        assertThat(decrypted).isEqualTo("test@example.com");
    }
}

