package com.kitchensink.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlphaOnlyValidator Tests")
class AlphaOnlyValidatorTest {

    private AlphaOnlyValidator validator;

    @Mock
    private AlphaOnly constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new AlphaOnlyValidator();
        validator.initialize(constraintAnnotation);
    }

    @Test
    @DisplayName("Should validate string with only letters")
    void testIsValid_LettersOnly() {
        assertThat(validator.isValid("John", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate string with letters and spaces")
    void testIsValid_LettersWithSpaces() {
        assertThat(validator.isValid("John Doe", context)).isTrue();
    }

    @Test
    @DisplayName("Should accept null value")
    void testIsValid_NullValue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("Should accept empty string")
    void testIsValid_EmptyString() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    @DisplayName("Should accept string with only spaces")
    void testIsValid_SpacesOnly() {
        assertThat(validator.isValid("   ", context)).isTrue();
    }

    @Test
    @DisplayName("Should reject string with numbers")
    void testIsValid_WithNumbers() {
        assertThat(validator.isValid("John123", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject string with special characters")
    void testIsValid_WithSpecialCharacters() {
        assertThat(validator.isValid("John-Doe", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject string with punctuation")
    void testIsValid_WithPunctuation() {
        assertThat(validator.isValid("John, Doe", context)).isFalse();
    }
}

