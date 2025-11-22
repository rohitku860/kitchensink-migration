package com.kitchensink.service;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InputSanitizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationService.class);
    
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String sanitized = input.trim();
        sanitized = StringEscapeUtils.escapeHtml4(sanitized);
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "");
        sanitized = sanitized.replaceAll("<[^>]+>", "");
        
        return sanitized;
    }
    
    public String sanitizeForEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        return email.trim().toLowerCase();
    }
    
    public String sanitizeForPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        return phone.replaceAll("[^0-9+]", "");
    }
    
    public String sanitizeForName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String sanitized = name.trim();
        sanitized = sanitized.replaceAll("[<>\"']", "");
        return sanitized;
    }
}

