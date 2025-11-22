package com.kitchensink.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("InputSanitizationService Unit Tests")
class InputSanitizationServiceTest {

    private InputSanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new InputSanitizationService();
    }

    @Test
    @DisplayName("Should sanitize name by escaping HTML")
    void testSanitizeForName() {
        // Given
        String input = "<script>alert('XSS')</script>John";

        // When
        String result = sanitizationService.sanitizeForName(input);

        // Then
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("</script>");
        assertThat(result).contains("John");
    }

    @Test
    @DisplayName("Should sanitize email by trimming and lowercasing")
    void testSanitizeForEmail() {
        // Given
        String input = "  TEST@EXAMPLE.COM  ";

        // When
        String result = sanitizationService.sanitizeForEmail(input);

        // Then
        assertThat(result).isEqualTo("test@example.com");
        assertThat(result).doesNotContain(" ");
    }

    @Test
    @DisplayName("Should sanitize phone by keeping only valid characters")
    void testSanitizeForPhone() {
        // Given
        String input = "123-456-7890<script>";

        // When
        String result = sanitizationService.sanitizeForPhone(input);

        // Then
        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("123");
        assertThat(result).contains("456");
        assertThat(result).contains("7890");
    }

    @Test
    @DisplayName("Should return null for null input")
    void testSanitizeForName_Null() {
        // When
        String result = sanitizationService.sanitizeForName(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve valid email characters")
    void testSanitizeForEmail_ValidCharacters() {
        // Given
        String input = "user.name+tag@example.co.uk";

        // When
        String result = sanitizationService.sanitizeForEmail(input);

        // Then
        assertThat(result).isEqualTo("user.name+tag@example.co.uk");
    }

    @Test
    @DisplayName("Should preserve valid phone characters")
    void testSanitizeForPhone_ValidCharacters() {
        // Given
        String input = "+1 (123) 456-7890";

        // When
        String result = sanitizationService.sanitizeForPhone(input);

        // Then
        assertThat(result).contains("1");
        assertThat(result).contains("123");
        assertThat(result).contains("456");
        assertThat(result).contains("7890");
    }
}

