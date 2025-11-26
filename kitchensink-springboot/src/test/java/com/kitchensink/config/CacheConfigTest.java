package com.kitchensink.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheConfig Tests")
class CacheConfigTest {

    @Test
    @DisplayName("Should create cache manager with all caches")
    void testCacheManager() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCache("userById")).isNotNull();
        assertThat(cacheManager.getCache("roleById")).isNotNull();
        assertThat(cacheManager.getCache("roleNameByUserId")).isNotNull();
    }

    @Test
    @DisplayName("Should cache user by ID")
    void testUserByIdCache() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();
        Cache cache = cacheManager.getCache("userById");

        assertThat(cache).isNotNull();
        cache.put("user-1", "test-value");
        assertThat(cache.get("user-1")).isNotNull();
    }

    @Test
    @DisplayName("Should cache role by ID")
    void testRoleByIdCache() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();
        Cache cache = cacheManager.getCache("roleById");

        assertThat(cache).isNotNull();
        cache.put("role-1", "ADMIN");
        assertThat(cache.get("role-1")).isNotNull();
    }

    @Test
    @DisplayName("Should cache role name by user ID")
    void testRoleNameByUserIdCache() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheManager cacheManager = cacheConfig.cacheManager();
        Cache cache = cacheManager.getCache("roleNameByUserId");

        assertThat(cache).isNotNull();
        cache.put("user-1", "USER");
        assertThat(cache.get("user-1")).isNotNull();
    }
}

