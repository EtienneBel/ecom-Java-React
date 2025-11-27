package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.dto.ProductDTO;
import com.ecommerce.sportscenter.entity.Product;
import com.ecommerce.sportscenter.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
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
 *
 * IMPORTANT: Uses TwoLevelCacheManager to warm BOTH L1 (Caffeine) AND L2 (Redis)
 */
@Service
@Slf4j
public class CacheWarmingService {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    public CacheWarmingService(
            ProductRepository productRepository,
            @Qualifier("compositeCacheManager") CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * WARM CACHE ON STARTUP
     *
     * @EventListener: Triggered when app is ready
     * ApplicationReadyEvent: After all beans initialized
     *
     * Now uses TwoLevelCacheManager to populate BOTH:
     * - L1 (Caffeine) for ultra-fast access
     * - L2 (Redis) for persistence and sharing
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("🔥 Starting cache warming (L1 + L2)...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Warm top products (most viewed)
            int productsWarmed = warmTopProducts();

            // 2. Warm all categories (menu navigation)
            int categoriesWarmed = warmCategories();

            // 3. Warm new products (homepage)
            int newProductsWarmed = warmNewProducts();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Cache warming completed in {} ms", duration);
            log.info("   └── Products: {}, Categories: {}, New Products: {}",
                    productsWarmed, categoriesWarmed, newProductsWarmed);

        } catch (Exception e) {
            log.error("❌ Cache warming failed", e);
            // Don't fail startup - continue with cold cache
        }
    }

    /**
     * WARM TOP PRODUCTS
     *
     * Strategy: Pre-cache most viewed products
     * Why? These will be requested first
     *
     * In production: Get from analytics/metrics
     * Here: Simulate with top 100
     *
     * Uses TwoLevelCacheManager → writes to L1 (Caffeine) + L2 (Redis)
     */
    private int warmTopProducts() {
        log.info("   Warming top products cache...");

        try {
            Cache productCache = cacheManager.getCache("productById");
            if (productCache == null) {
                log.warn("   Cache 'productById' not found");
                return 0;
            }

            // Get active products (simulate top 100)
            List<Product> topProducts = productRepository.findByActiveTrue()
                    .stream()
                    .limit(100)
                    .collect(Collectors.toList());

            int cached = 0;
            for (Product product : topProducts) {
                ProductDTO dto = convertToDTO(product);
                // Uses TwoLevelCacheManager.put() → writes to BOTH L1 and L2
                productCache.put(product.getId(), dto);
                cached++;
            }

            log.info("   └── Warmed {} top products (L1 + L2)", cached);
            return cached;

        } catch (Exception e) {
            log.error("   Failed to warm top products", e);
            return 0;
        }
    }

    /**
     * WARM CATEGORIES
     *
     * Strategy: Pre-cache all categories
     * Why? Used in navigation menu (every page)
     *
     * Uses TwoLevelCacheManager → writes to L1 (Caffeine) + L2 (Redis)
     */
    private int warmCategories() {
        log.info("   Warming categories cache...");

        try {
            Cache categoriesCache = cacheManager.getCache("categories");
            Cache productsCache = cacheManager.getCache("products");

            if (categoriesCache == null) {
                log.warn("   Cache 'categories' not found");
                return 0;
            }

            List<String> categories = productRepository.findAllCategories();

            // Cache categories list with a simple key
            categoriesCache.put("all", categories);

            log.info("   └── Warmed {} categories (L1 + L2)", categories.size());

            // Also warm products by category
            if (productsCache != null) {
                for (String category : categories) {
                    List<Product> products = productRepository.findByCategory(category);
                    List<ProductDTO> dtos = products.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());

                    // Uses TwoLevelCacheManager.put() → writes to BOTH L1 and L2
                    productsCache.put("category:" + category, dtos);
                }
                log.info("   └── Warmed products for {} categories (L1 + L2)", categories.size());
            }

            return categories.size();

        } catch (Exception e) {
            log.error("   Failed to warm categories", e);
            return 0;
        }
    }

    /**
     * WARM NEW PRODUCTS
     *
     * Strategy: Pre-cache newest products
     * Why? Shown on homepage
     *
     * Uses TwoLevelCacheManager → writes to L1 (Caffeine) + L2 (Redis)
     */
    private int warmNewProducts() {
        log.info("   Warming new products cache...");

        try {
            Cache productsCache = cacheManager.getCache("products");
            if (productsCache == null) {
                log.warn("   Cache 'products' not found");
                return 0;
            }

            List<Product> newProducts = productRepository.findTop10ByOrderByCreatedAtDesc();
            List<ProductDTO> dtos = newProducts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Uses TwoLevelCacheManager.put() → writes to BOTH L1 and L2
            productsCache.put("new", dtos);

            log.info("   └── Warmed {} new products (L1 + L2)", dtos.size());
            return dtos.size();

        } catch (Exception e) {
            log.error("   Failed to warm new products", e);
            return 0;
        }
    }

    /**
     * MANUAL CACHE WARMING ENDPOINT
     *
     * Usage: Admin can trigger re-warming
     * Ex: After bulk product import
     */
    public void manualWarmCache() {
        log.info("🔄 Manual cache warming triggered...");
        warmCacheOnStartup();
    }

    /**
     * CLEAR ALL CACHES
     *
     * Usage: Admin can clear all caches
     * Ex: After major DB updates
     *
     * Uses TwoLevelCacheManager.clear() → clears BOTH L1 and L2
     */
    public void clearAllCaches() {
        log.info("🗑️ Clearing all caches (L1 + L2)...");

        try {
            String[] cacheNames = {"productById", "products", "categories", "searchResults", "priceRange"};

            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear(); // TwoLevelCache.clear() clears BOTH L1 and L2
                    log.info("   └── Cleared cache: {}", cacheName);
                }
            }

            log.info("✅ All caches cleared (L1 + L2)");

        } catch (Exception e) {
            log.error("❌ Failed to clear caches", e);
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