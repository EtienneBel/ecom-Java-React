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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductService - Business Logic (NO CACHE VERSION)
 *
 * This is the baseline version WITHOUT any caching.
 * Every request goes directly to the database.
 *
 * Used for benchmark comparison with the cached version.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final MeterRegistry meterRegistry;

    // Metrics counters
    private Counter databaseQueryCounter;

    /**
     * POST-CONSTRUCT: Initialize metrics
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        databaseQueryCounter = Counter.builder("database.query")
                .description("Database query count")
                .tag("source", "product_service")
                .register(meterRegistry);
    }

    /**
     * GET PRODUCT BY ID - Direct DB access (no cache)
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);

        log.debug("Fetching product {} from DB", id);
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
     * GET PRODUCT BY ID WITH LOCK - Same as getProductById (no lock needed without cache)
     */
    public ProductDTO getProductByIdWithLock(Long id) {
        return getProductById(id);
    }

    /**
     * GET ALL PRODUCTS - Direct DB access
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.debug("Fetching all products from DB");
        databaseQueryCounter.increment();

        return productRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET PRODUCTS BY CATEGORY - Direct DB access
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.debug("Fetching products for category: {}", category);
        databaseQueryCounter.increment();

        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * SEARCH PRODUCTS - Direct DB access
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        log.debug("Searching products with keyword: {}", keyword);
        databaseQueryCounter.increment();

        return productRepository.searchProducts(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET PRODUCTS BY PRICE RANGE - Direct DB access
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Fetching products in price range: {} - {}", minPrice, maxPrice);
        databaseQueryCounter.increment();

        return productRepository.findByPriceRange(minPrice, maxPrice).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET ALL CATEGORIES - Direct DB access
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        log.debug("Fetching all categories from DB");
        databaseQueryCounter.increment();

        return productRepository.findAllCategories();
    }

    /**
     * CREATE PRODUCT
     */
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating new product: {}", productDTO.getName());

        Product product = convertToEntity(productDTO);
        Product saved = productRepository.save(product);

        return convertToDTO(saved);
    }

    /**
     * UPDATE PRODUCT
     */
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Updating product: {}", id);

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
     * DELETE PRODUCT
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);
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
