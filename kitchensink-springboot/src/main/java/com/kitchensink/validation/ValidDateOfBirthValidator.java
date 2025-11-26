package com.kitchensink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidDateOfBirthValidator implements ConstraintValidator<ValidDateOfBirth, String> {
    
    private static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
    
    @Override
    public void initialize(ValidDateOfBirth constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
            return true;
        }
        
        try {
            LocalDate date = LocalDate.parse(dateOfBirth, formatter);
            LocalDate today = LocalDate.now();
            LocalDate hundredYearsAgo = today.minusYears(100);
            
            if (date.isAfter(today)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Date of birth cannot be a future date")
                        .addConstraintViolation();
                return false;
            }
            
            if (date.isBefore(hundredYearsAgo)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Date of birth cannot be more than 100 years ago")
                        .addConstraintViolation();
                return false;
            }
            
            return true;
        } catch (DateTimeParseException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Date of birth must be in DD-MM-YYYY format")
                    .addConstraintViolation();
            return false;
        }
    }
}

