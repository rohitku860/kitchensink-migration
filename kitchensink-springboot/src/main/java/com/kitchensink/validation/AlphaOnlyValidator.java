package com.kitchensink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AlphaOnlyValidator implements ConstraintValidator<AlphaOnly, String> {
    
    private static final String ALPHA_ONLY_PATTERN = "^[a-zA-Z\\s]+$";
    
    @Override
    public void initialize(AlphaOnly constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return value.matches(ALPHA_ONLY_PATTERN);
    }
}

