package com.ecommerce.sportscenter.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Cache Configuration using CompositeCacheManager
 *
 * Testing if CompositeCacheManager provides TRUE multi-level caching
 *
 * CompositeCacheManager behavior:
 * - Iterates through cache managers in order
 * - Returns first cache found with matching name
 * - Does NOT populate L1 from L2 hits
 * - Does NOT write to both levels
 */
@Configuration
public class MultiLevelCacheConfig {

    @Bean
    @Primary
    public CacheManager compositeCacheManager(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager) {

        CompositeCacheManager cacheManager = new CompositeCacheManager(
                caffeineCacheManager,  // L1 - Caffeine (checked first)
                redisCacheManager      // L2 - Redis (fallback)
        );
        cacheManager.setFallbackToNoOpCache(false);
        return cacheManager;
    }
}
