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
        
        // Regex: <script[^>]*>.*?</script>
        // Explanation:
        //   <script    - Matches literal "<script"
        //   [^>]*      - Matches zero or more characters that are NOT '>' (allows attributes like <script src="...">)
        //   >          - Matches the closing '>' of opening tag
        //   .*?       - Matches any characters (non-greedy, as few as possible)
        //   </script> - Matches literal closing "</script>" tag
        // Purpose: Removes entire <script> tags and their content to prevent XSS attacks
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "");
        
        // Regex: <[^>]+>
        // Explanation:
        //   <         - Matches literal '<'
        //   [^>]+     - Matches one or more characters that are NOT '>' (the tag name and attributes)
        //   >         - Matches literal '>'
        // Purpose: Removes all remaining HTML tags (e.g., <div>, <span>, <img>, etc.) to prevent HTML injection
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
        
        // Regex: [^0-9+]
        // Explanation:
        //   [^...]    - Character class negation (matches anything NOT in the brackets)
        //   0-9       - Matches digits 0 through 9
        //   +         - Matches literal '+' character (for international format like +1234567890)
        // Purpose: Removes all characters except digits and plus sign, keeping only valid phone number characters
        // Example: "(123) 456-7890" becomes "1234567890", "+1-234-567-8900" becomes "+12345678900"
        return phone.replaceAll("[^0-9+]", "");
    }
    
    public String sanitizeForName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String sanitized = name.trim();
        
        // First, remove <script> tags and their content (critical for XSS prevention)
        // Regex: <script[^>]*>.*?</script>
        // Purpose: Removes entire <script> tags and their content to prevent XSS attacks
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "");
        
        // Then remove all other HTML tags
        // Regex: <[^>]+>
        // Purpose: Removes all remaining HTML tags (e.g., <div>, <span>, <img>, etc.)
        sanitized = sanitized.replaceAll("<[^>]+>", "");
        
        // Finally, remove dangerous characters that could be used in HTML attributes
        // Regex: [<>\"']
        // Explanation:
        //   [...]     - Character class (matches any single character in the brackets)
        //   <         - Matches literal '<' (prevents HTML tag injection)
        //   >         - Matches literal '>' (prevents HTML tag injection)
        //   \"        - Matches literal double quote '"' (prevents attribute injection)
        //   '         - Matches literal single quote "'" (prevents attribute injection)
        // Purpose: Removes HTML brackets and quotes to prevent XSS and HTML injection attacks
        // Note: This is done after tag removal to catch any remaining dangerous characters
        // Example: "John<script>" becomes "Johnscript", "O'Brien" becomes "OBrien"
        sanitized = sanitized.replaceAll("[<>\"']", "");
        
        return sanitized;
    }
}

