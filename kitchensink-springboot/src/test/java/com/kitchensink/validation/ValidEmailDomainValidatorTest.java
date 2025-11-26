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
@DisplayName("ValidEmailDomainValidator Tests")
class ValidEmailDomainValidatorTest {

    private ValidEmailDomainValidator validator;

    @Mock
    private ValidEmailDomain constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidEmailDomainValidator();
        validator.initialize(constraintAnnotation);
    }

    @Test
    @DisplayName("Should validate valid email with proper domain")
    void testIsValid_ValidEmail() {
        assertThat(validator.isValid("test@example.com", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate email with subdomain")
    void testIsValid_EmailWithSubdomain() {
        assertThat(validator.isValid("user@mail.example.com", context)).isTrue();
    }

    @Test
    @DisplayName("Should reject null email")
    void testIsValid_NullEmail() {
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject empty email")
    void testIsValid_EmptyEmail() {
        assertThat(validator.isValid("", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email without @ symbol")
    void testIsValid_EmailWithoutAt() {
        assertThat(validator.isValid("testexample.com", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email with multiple @ symbols")
    void testIsValid_EmailWithMultipleAt() {
        assertThat(validator.isValid("test@@example.com", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email with domain less than 4 characters")
    void testIsValid_ShortDomain() {
        assertThat(validator.isValid("test@ab.c", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email without TLD")
    void testIsValid_EmailWithoutTLD() {
        assertThat(validator.isValid("test@example", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email with TLD less than 2 characters")
    void testIsValid_ShortTLD() {
        assertThat(validator.isValid("test@example.c", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject email with numeric TLD")
    void testIsValid_NumericTLD() {
        assertThat(validator.isValid("test@example.123", context)).isFalse();
    }

    @Test
    @DisplayName("Should validate email with valid TLD")
    void testIsValid_ValidTLD() {
        assertThat(validator.isValid("test@example.org", context)).isTrue();
    }
}

