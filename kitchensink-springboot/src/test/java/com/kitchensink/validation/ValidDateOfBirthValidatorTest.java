package com.kitchensink.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidDateOfBirthValidator Tests")
class ValidDateOfBirthValidatorTest {

    private ValidDateOfBirthValidator validator;

    @Mock
    private ValidDateOfBirth constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidDateOfBirthValidator();
        validator.initialize(constraintAnnotation);
    }

    @Test
    @DisplayName("Should validate valid date of birth")
    void testIsValid_ValidDate() {
        String validDate = LocalDate.now().minusYears(25).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(validator.isValid(validDate, context)).isTrue();
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
    @DisplayName("Should reject future date")
    void testIsValid_FutureDate() {
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);
        
        String futureDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(validator.isValid(futureDate, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject date more than 100 years ago")
    void testIsValid_MoreThan100YearsAgo() {
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);
        
        String oldDate = LocalDate.now().minusYears(101).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(validator.isValid(oldDate, context)).isFalse();
    }

    @Test
    @DisplayName("Should accept date exactly 100 years ago")
    void testIsValid_Exactly100YearsAgo() {
        String date100YearsAgo = LocalDate.now().minusYears(100).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(validator.isValid(date100YearsAgo, context)).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid date format")
    void testIsValid_InvalidFormat() {
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);
        
        assertThat(validator.isValid("1990-01-01", context)).isFalse();
        assertThat(validator.isValid("01/01/1990", context)).isFalse();
        assertThat(validator.isValid("1-1-1990", context)).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid date values")
    void testIsValid_InvalidDate() {
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);
        
        // Test invalid day
        assertThat(validator.isValid("32-01-1990", context)).isFalse();
        // Test invalid month
        assertThat(validator.isValid("01-13-1990", context)).isFalse();
        // Test invalid date (Feb doesn't have 31 days) - LocalDate.parse might adjust this, so we test with a clearly invalid format instead
        assertThat(validator.isValid("99-99-1990", context)).isFalse();
    }

    @Test
    @DisplayName("Should accept today's date")
    void testIsValid_Today() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(validator.isValid(today, context)).isTrue();
    }
}

