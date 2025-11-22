package com.kitchensink.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.api")
public class ApiKeyConfig {
    
    private String key;
    private boolean enabled = true;
    private List<String> allowedOrigins;
    private List<String> publicPaths;
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
    
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
    
    public List<String> getPublicPaths() {
        return publicPaths;
    }
    
    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}

