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
@DisplayName("IndianIsdCodeValidator Tests")
class IndianIsdCodeValidatorTest {

    private IndianIsdCodeValidator validator;

    @Mock
    private IndianIsdCode constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new IndianIsdCodeValidator();
        validator.initialize(constraintAnnotation);
    }

    @Test
    @DisplayName("Should validate +91 ISD code")
    void testIsValid_Plus91() {
        assertThat(validator.isValid("+91", context)).isTrue();
    }

    @Test
    @DisplayName("Should validate 91 ISD code")
    void testIsValid_91() {
        assertThat(validator.isValid("91", context)).isTrue();
    }

    @Test
    @DisplayName("Should accept null value")
    void testIsValid_Null() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("Should accept empty string")
    void testIsValid_Empty() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    @DisplayName("Should accept string with only spaces")
    void testIsValid_SpacesOnly() {
        assertThat(validator.isValid("   ", context)).isTrue();
    }

    @Test
    @DisplayName("Should reject other ISD codes")
    void testIsValid_OtherISDCodes() {
        assertThat(validator.isValid("+1", context)).isFalse();
        assertThat(validator.isValid("+44", context)).isFalse();
        assertThat(validator.isValid("+86", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid format")
    void testIsValid_InvalidFormat() {
        assertThat(validator.isValid("91+", context)).isFalse();
        assertThat(validator.isValid("+911", context)).isFalse();
        assertThat(validator.isValid("0091", context)).isFalse();
    }
}

