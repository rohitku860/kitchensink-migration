package com.kitchensink.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        Cache userByIdCache = new CaffeineCache("userById", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        Cache roleByIdCache = new CaffeineCache("roleById", Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        Cache roleByNameCache = new CaffeineCache("roleByName", Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        Cache roleNameByUserIdCache = new CaffeineCache("roleNameByUserId", Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        Cache userIdsByRoleIdCache = new CaffeineCache("userIdsByRoleId", Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        cacheManager.setCaches(Arrays.asList(userByIdCache, roleByIdCache, roleByNameCache, roleNameByUserIdCache, userIdsByRoleIdCache));
        return cacheManager;
    }
}

