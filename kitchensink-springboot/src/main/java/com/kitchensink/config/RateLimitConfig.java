package com.kitchensink.config;

import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    
    private final Map<String, RateLimitInfo> cache = new ConcurrentHashMap<>();
    
    public boolean isAllowed(String key) {
        RateLimitInfo info = cache.computeIfAbsent(key, k -> new RateLimitInfo());
        LocalDateTime now = LocalDateTime.now();
        
        if (info.getWindowStart().plusMinutes(1).isBefore(now)) {
            info.reset();
        }
        
        if (info.getCount() >= DEFAULT_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        info.increment();
        return true;
    }
    
    public void resetBucket(String key) {
        cache.remove(key);
    }
    
    private static class RateLimitInfo {
        private int count = 0;
        private LocalDateTime windowStart = LocalDateTime.now();
        
        public void increment() {
            count++;
        }
        
        public void reset() {
            count = 0;
            windowStart = LocalDateTime.now();
        }
        
        public int getCount() {
            return count;
        }
        
        public LocalDateTime getWindowStart() {
            return windowStart;
        }
    }
}

