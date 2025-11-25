package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.dto.ProductDTO;
import com.ecommerce.sportscenter.entity.Product;
import com.ecommerce.sportscenter.exception.ProductNotFoundException;
import com.ecommerce.sportscenter.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ProductService - Business Logic avec Multi-Level Caching
 *
 * 🎯 PATTERNS IMPLÉMENTÉS:
 * 1. Cache-Aside Pattern (lazy loading)
 * 2. Cache Stampede Prevention (distributed locks)
 * 3. Multi-Level Caching (Caffeine → Redis → DB)
 * 4. Metrics Collection (Micrometer)
 *
 * FLOW TYPIQUE:
 * Request → Check Caffeine → Check Redis → Query DB → Cache result
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    // Metrics counters
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Counter databaseQueryCounter;

    /**
     * POST-CONSTRUCT: Initialize metrics
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        cacheHitCounter = Counter.builder("cache.hit")
                .description("Cache hit count")
                .tag("cache", "multi-level")
                .register(meterRegistry);

        cacheMissCounter = Counter.builder("cache.miss")
                .description("Cache miss count")
                .tag("cache", "multi-level")
                .register(meterRegistry);

        databaseQueryCounter = Counter.builder("database.query")
                .description("Database query count")
                .tag("source", "product_service")
                .register(meterRegistry);
    }

    /**
     * GET PRODUCT BY ID - Multi-Level Caching
     *
     * @Cacheable:
     * - key = "product:#{id}"
     * - cacheManager = Redis (L2)
     * - Caffeine (L1) check automatique si configuré
     *
     * FLOW:
     * 1. Check Caffeine (L1) → Hit? Return
     * 2. Check Redis (L2) → Hit? Return + Cache in Caffeine
     * 3. Query DB → Cache in Redis + Caffeine
     */
    @Cacheable(value = "productById", key = "#id")
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);

        log.info(" Fetching product {} - Missed all caches, querying DB", id);
        cacheMissCounter.increment();
        databaseQueryCounter.increment();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        ProductDTO dto = convertToDTO(product);
        dto.setCacheSource("DATABASE");

        sample.stop(Timer.builder("product.fetch.time")
                .tag("source", "database")
                .register(meterRegistry));

        return dto;
    }

    /**
     * GET PRODUCT BY ID WITH DISTRIBUTED LOCK
     *
     * 🎯 PROBLEM: Cache Stampede
     * Scénario: Cache expire, 1000 requests simultanées
     * Sans lock: 1000 queries DB 😱
     * Avec lock: 1 query DB, 999 wait for result 🎉
     *
     * REDISSON LOCK:
     * - Distributed (partagé entre tous les serveurs)
     * - Auto-release (si crash)
     * - Fair lock (FIFO)
     */
    public ProductDTO getProductByIdWithLock(Long id) {
        String lockKey = "lock:product:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock (wait max 5s, auto-release after 10s)
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    log.info("🔒 Lock acquired for product {}", id);

                    // Check cache again (peut-être rempli pendant l'attente)
                    String cacheKey = "product:" + id;
                    ProductDTO cached = (ProductDTO) redisTemplate.opsForValue().get(cacheKey);

                    if (cached != null) {
                        log.info("✅ Cache hit after lock wait for product {}", id);
                        cacheHitCounter.increment();
                        cached.setCacheSource("L2_REDIS_AFTER_LOCK");
                        return cached;
                    }

                    // Query DB (seul ce thread le fait)
                    log.info("📊 Querying DB for product {} with lock", id);
                    ProductDTO result = getProductById(id);

                    // Cache result
                    redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);

                    return result;

                } finally {
                    lock.unlock();
                    log.info("🔓 Lock released for product {}", id);
                }
            } else {
                // Couldn't acquire lock - fallback to normal fetch
                log.warn("⚠️ Couldn't acquire lock for product {}, fallback", id);
                return getProductById(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        }
    }

    /**
     * GET ALL PRODUCTS - Cacheable
     */
    @Cacheable(value = "products")
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info(" Fetching all products from DB");
        databaseQueryCounter.increment();

        return productRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET PRODUCTS BY CATEGORY - Cacheable
     * key = "category:#{category}"
     */
    @Cacheable(value = "products", key = "'category:' + #category")
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info(" Fetching products for category: {}", category);
        databaseQueryCounter.increment();

        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * SEARCH PRODUCTS - Short TTL cache
     */
    @Cacheable(value = "searchResults", key = "#keyword")
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        log.info(" Searching products with keyword: {}", keyword);
        databaseQueryCounter.increment();

        return productRepository.searchProducts(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET PRODUCTS BY PRICE RANGE
     */
    @Cacheable(value = "priceRange",
            key = "'price:' + #minPrice + '-' + #maxPrice",
            cacheManager = "redisCacheManager")
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info(" Fetching products in price range: {} - {}", minPrice, maxPrice);
        databaseQueryCounter.increment();

        return productRepository.findByPriceRange(minPrice, maxPrice).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET ALL CATEGORIES - Long TTL cache
     */
    @Cacheable(value = "categories")
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        log.info(" Fetching all categories from DB");
        databaseQueryCounter.increment();

        return productRepository.findAllCategories();
    }

    /**
     * CREATE PRODUCT - Invalidate relevant caches
     *
     * @CacheEvict: Remove cache entries
     * allEntries=true: Clear entire cache (prudent pour liste)
     */
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "priceRange", allEntries = true)
    })
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("➕ Creating new product: {}", productDTO.getName());

        Product product = convertToEntity(productDTO);
        Product saved = productRepository.save(product);

        return convertToDTO(saved);
    }

    /**
     * UPDATE PRODUCT - Update cache (Cache-Put)
     *
     * @CachePut: Update cache with new value
     * Différent de @Cacheable (qui skip method si cached)
     */
    @Caching(
            put = @CachePut(value = "productById", key = "#id"),
            evict = {
                    @CacheEvict(value = "products", allEntries = true),
                    @CacheEvict(value = "priceRange", allEntries = true)
            }
    )
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("🔄 Updating product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Update fields
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setCategory(productDTO.getCategory());
        product.setBrand(productDTO.getBrand());
        product.setImageUrl(productDTO.getImageUrl());
        product.setActive(productDTO.getActive());

        Product updated = productRepository.save(product);
        return convertToDTO(updated);
    }

    /**
     * DELETE PRODUCT - Clear all related caches
     */
    @Caching(evict = {
            @CacheEvict(value = "productById", key = "#id"),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "priceRange", allEntries = true),
            @CacheEvict(value = "searchResults", allEntries = true)
    })
    @Transactional
    public void deleteProduct(Long id) {
        log.info("🗑️ Deleting product: {}", id);
        productRepository.deleteById(id);
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
                .build();
    }

    /**
     * Helper: Convert DTO to Entity
     */
    private Product convertToEntity(ProductDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .category(dto.getCategory())
                .brand(dto.getBrand())
                .imageUrl(dto.getImageUrl())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}