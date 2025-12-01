package com.kitchensink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cache eviction tasks
 */
@Service
public class CacheEvictionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionService.class);
    private final CacheManager cacheManager;
    
    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    /**
     * Evict all role-related caches at midnight (00:00:00) every day
     * This ensures role caches are refreshed at least once per day
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight (00:00:00) every day
    @CacheEvict(value = {"roleById", "roleByName", "userIdsByRoleId"}, allEntries = true)
    public void evictRoleCachesAtMidnight() {
        logger.info("Scheduled cache eviction: Clearing all role-related caches at midnight");
        
        // Explicitly evict caches programmatically as well
        evictCache("roleById");
        evictCache("roleByName");
        evictCache("userIdsByRoleId");
        
        logger.info("Role caches evicted successfully at midnight");
    }
    
    /**
     * Helper method to evict a specific cache
     */
    private void evictCache(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cache '{}' cleared", cacheName);
            } else {
                logger.warn("Cache '{}' not found in cache manager", cacheName);
            }
        } catch (Exception e) {
            logger.error("Error evicting cache '{}': {}", cacheName, e.getMessage(), e);
        }
    }
}

