package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.dto.ProductDTO;
import com.ecommerce.sportscenter.entity.Product;
import com.ecommerce.sportscenter.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CacheWarmingService - Pre-populate caches at startup
 *
 * CONCEPT: Cache Warming
 *
 * PROBLEM:
 * - Application starts → Caches empty (cold start)
 * - First requests slow (query DB)
 * - Bad user experience
 *
 * SOLUTION:
 * - Pre-load hot data at startup
 * - Top products, categories, etc.
 * - First users get fast responses immediately
 *
 * QUAND UTILISER?
 * Application restart (deploy, scale-up)
 * Predictable hot data (top 100 products)
 * Off-peak hours (less impact)
 *
 * TRADE-OFFS:
 * Slower startup time
 * Peut cacher données obsolètes si DB pas à jour
 * Much better user experience
 * Prevents cache stampede on startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * WARM CACHE ON STARTUP
     *
     * @EventListener: Triggered when app is ready
     * ApplicationReadyEvent: After all beans initialized
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("Cache warming is disabled - skipping");
        return;
        /*
        log.info("Starting cache warming...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Warm top products (most viewed)
            warmTopProducts();

            // 2. Warm all categories (menu navigation)
            warmCategories();

            // 3. Warm new products (homepage)
            warmNewProducts();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warming completed in {} ms", duration);

        } catch (Exception e) {
            log.error("Cache warming failed", e);
            // Don't fail startup - continue with cold cache
        }
        */
    }

    /**
     * WARM TOP PRODUCTS
     *
     * Strategy: Pre-cache most viewed products
     * Why? These will be requested first
     *
     * In production: Get from analytics/metrics
     * Here: Simulate with top 100
     */
    private void warmTopProducts() {
        log.info("Warming top products cache...");

        try {
            // Get active products (simulate top 100)
            List<Product> topProducts = productRepository.findByActiveTrue()
                    .stream()
                    .limit(100)
                    .collect(Collectors.toList());

            int cached = 0;
            for (Product product : topProducts) {
                String cacheKey = "productById::" + product.getId();
                ProductDTO dto = convertToDTO(product);

                // Cache in Redis with 15 min TTL
                redisTemplate.opsForValue().set(
                        cacheKey,
                        dto,
                        15,
                        TimeUnit.MINUTES
                );

                cached++;
            }

            log.info("Warmed {} top products", cached);

        } catch (Exception e) {
            log.error("Failed to warm top products", e);
        }
    }

    /**
     * WARM CATEGORIES
     *
     * Strategy: Pre-cache all categories
     * Why? Used in navigation menu (every page)
     */
    private void warmCategories() {
        log.info("Warming categories cache...");

        try {
            List<String> categories = productRepository.findAllCategories();
            String cacheKey = "categories::SimpleKey []";

            // Cache with 1 hour TTL (very stable data)
            redisTemplate.opsForValue().set(
                    cacheKey,
                    categories,
                    1,
                    TimeUnit.HOURS
            );

            log.info("Warmed {} categories", categories.size());

            // Also warm products by category
            for (String category : categories) {
                List<Product> products = productRepository.findByCategory(category);
                List<ProductDTO> dtos = products.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

                String categoryCacheKey = "products::category:" + category;
                redisTemplate.opsForValue().set(
                        categoryCacheKey,
                        dtos,
                        10,
                        TimeUnit.MINUTES
                );
            }

            log.info("Warmed products for all categories");

        } catch (Exception e) {
            log.error("Failed to warm categories", e);
        }
    }

    /**
     * WARM NEW PRODUCTS
     *
     * Strategy: Pre-cache newest products
     * Why? Shown on homepage
     */
    private void warmNewProducts() {
        log.info("Warming new products cache...");

        try {
            List<Product> newProducts = productRepository.findTop10ByOrderByCreatedAtDesc();
            List<ProductDTO> dtos = newProducts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            String cacheKey = "products::new";
            redisTemplate.opsForValue().set(
                    cacheKey,
                    dtos,
                    5,
                    TimeUnit.MINUTES
            );

            log.info("Warmed {} new products", dtos.size());

        } catch (Exception e) {
            log.error("Failed to warm new products", e);
        }
    }

    /**
     * MANUAL CACHE WARMING ENDPOINT
     *
     * Usage: Admin can trigger re-warming
     * Ex: After bulk product import
     */
    public void manualWarmCache() {
        log.info("Manual cache warming triggered...");
        warmCacheOnStartup();
    }

    /**
     * CLEAR ALL CACHES
     *
     * Usage: Admin can clear all caches
     * Ex: After major DB updates
     */
    public void clearAllCaches() {
        log.info("🗑️ Clearing all caches...");

        try {
            // Clear Redis (all keys matching patterns)
            redisTemplate.delete(redisTemplate.keys("productById::*"));
            redisTemplate.delete(redisTemplate.keys("products::*"));
            redisTemplate.delete(redisTemplate.keys("categories::*"));
            redisTemplate.delete(redisTemplate.keys("searchResults::*"));
            redisTemplate.delete(redisTemplate.keys("priceRange::*"));

            log.info("All caches cleared");

        } catch (Exception e) {
            log.error("Failed to clear caches", e);
        }
    }

    /**
     * Helper: Convert Entity to DTO
     */
    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .cacheSource("WARMED")
                .build();
    }
}