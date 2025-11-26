package com.kitchensink.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndianMobileNumberValidator Tests")
class IndianMobileNumberValidatorTest {

    private IndianMobileNumberValidator validator;

    @Mock
    private IndianMobileNumber constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new IndianMobileNumberValidator();
        validator.initialize(constraintAnnotation);
    }

    @Test
    @DisplayName("Should validate valid Indian mobile number starting with 6")
    void testIsValid_ValidNumberStartingWith6() {
        assertThat(validator.isValid("6123456789", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate valid Indian mobile number starting with 7")
    void testIsValid_ValidNumberStartingWith7() {
        assertThat(validator.isValid("7123456789", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate valid Indian mobile number starting with 8")
    void testIsValid_ValidNumberStartingWith8() {
        assertThat(validator.isValid("8123456789", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate valid Indian mobile number starting with 9")
    void testIsValid_ValidNumberStartingWith9() {
        assertThat(validator.isValid("9123456789", context)).isTrue();
    }

    @Test
    @DisplayName("Should reject number starting with 0")
    void testIsValid_InvalidNumberStartingWith0() {
        assertThat(validator.isValid("0123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number starting with 1")
    void testIsValid_InvalidNumberStartingWith1() {
        assertThat(validator.isValid("1123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number starting with 2")
    void testIsValid_InvalidNumberStartingWith2() {
        assertThat(validator.isValid("2123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number starting with 3")
    void testIsValid_InvalidNumberStartingWith3() {
        assertThat(validator.isValid("3123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number starting with 4")
    void testIsValid_InvalidNumberStartingWith4() {
        assertThat(validator.isValid("4123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number starting with 5")
    void testIsValid_InvalidNumberStartingWith5() {
        assertThat(validator.isValid("5123456789", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number with less than 10 digits")
    void testIsValid_InvalidNumberLessThan10Digits() {
        assertThat(validator.isValid("987654321", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject number with more than 10 digits")
    void testIsValid_InvalidNumberMoreThan10Digits() {
        assertThat(validator.isValid("98765432101", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject null phone number")
    void testIsValid_NullPhoneNumber() {
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject empty phone number")
    void testIsValid_EmptyPhoneNumber() {
        assertThat(validator.isValid("", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject phone number with only spaces")
    void testIsValid_WhitespaceOnly() {
        assertThat(validator.isValid("   ", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject phone number with non-numeric characters")
    void testIsValid_NonNumericCharacters() {
        assertThat(validator.isValid("98765abcde", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject phone number with special characters")
    void testIsValid_SpecialCharacters() {
        assertThat(validator.isValid("98765-4321", context)).isFalse();
    }
}

