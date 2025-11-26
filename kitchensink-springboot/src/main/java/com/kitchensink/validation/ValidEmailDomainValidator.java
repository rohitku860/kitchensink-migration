package com.kitchensink.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidEmailDomainValidator implements ConstraintValidator<ValidEmailDomain, String> {
    
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    
    @Override
    public void initialize(ValidEmailDomain constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        if (!pattern.matcher(email).matches()) {
            return false;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }
        
        String domain = parts[1];
        if (domain.length() < 4) {
            return false;
        }
        
        if (!domain.contains(".")) {
            return false;
        }
        
        String[] domainParts = domain.split("\\.");
        if (domainParts.length < 2) {
            return false;
        }
        
        String tld = domainParts[domainParts.length - 1];
        return tld.length() >= 2 && tld.matches("^[a-zA-Z]+$");
    }
}

