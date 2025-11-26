package com.kitchensink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IndianIsdCodeValidator implements ConstraintValidator<IndianIsdCode, String> {
    
    @Override
    public void initialize(IndianIsdCode constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String isdCode, ConstraintValidatorContext context) {
        if (isdCode == null || isdCode.trim().isEmpty()) {
            return true;
        }
        return isdCode.equals("+91") || isdCode.equals("91");
    }
}

