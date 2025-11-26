package com.kitchensink.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitConfig Tests")
class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }

    @Test
    @DisplayName("Should allow request when under limit")
    void testIsAllowed_UnderLimit() {
        boolean result = rateLimitConfig.isAllowed("127.0.0.1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow multiple requests up to limit")
    void testIsAllowed_MultipleRequests() {
        String key = "127.0.0.1";
        
        // Make 60 requests (the limit)
        for (int i = 0; i < 60; i++) {
            boolean allowed = rateLimitConfig.isAllowed(key);
            assertThat(allowed).isTrue();
        }
        
        // 61st request should be blocked
        boolean blocked = rateLimitConfig.isAllowed(key);
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("Should reset bucket successfully")
    void testResetBucket() {
        String key = "127.0.0.1";
        
        // Make requests to fill the bucket
        for (int i = 0; i < 60; i++) {
            rateLimitConfig.isAllowed(key);
        }
        
        // Should be blocked
        assertThat(rateLimitConfig.isAllowed(key)).isFalse();
        
        // Reset bucket
        rateLimitConfig.resetBucket(key);
        
        // Should be allowed again
        assertThat(rateLimitConfig.isAllowed(key)).isTrue();
    }

    @Test
    @DisplayName("Should handle different IP addresses separately")
    void testIsAllowed_DifferentIPs() {
        String ip1 = "127.0.0.1";
        String ip2 = "192.168.1.1";
        
        // Fill bucket for IP1
        for (int i = 0; i < 60; i++) {
            rateLimitConfig.isAllowed(ip1);
        }
        
        // IP1 should be blocked
        assertThat(rateLimitConfig.isAllowed(ip1)).isFalse();
        
        // IP2 should still be allowed
        assertThat(rateLimitConfig.isAllowed(ip2)).isTrue();
    }
}

