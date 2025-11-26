package com.kitchensink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IndianMobileNumberValidator implements ConstraintValidator<IndianMobileNumber, String> {
    
    private static final String INDIAN_MOBILE_PATTERN = "^[6-9]\\d{9}$";
    
    @Override
    public void initialize(IndianMobileNumber constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return phoneNumber.matches(INDIAN_MOBILE_PATTERN);
    }
}

