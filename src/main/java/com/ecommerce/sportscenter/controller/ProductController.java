package com.ecommerce.sportscenter.controller;

import com.ecommerce.sportscenter.dto.ProductDTO;
import com.ecommerce.sportscenter.service.ProductService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * ProductController - REST API Endpoints
 *
 * ENDPOINTS E-COMMERCE:
 * - GET /products → Catalogue complet
 * - GET /products/{id} → Détail produit (HOT PATH - très caché)
 * - GET /products/category/{category} → Filtrage
 * - GET /products/search → Recherche
 * - POST /products → Création
 *
 * @Timed: Métrique Micrometer pour Prometheus
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * GET ALL PRODUCTS
     * Usage: Page d'accueil, liste complète
     * Cache: Redis 10 min
     */
    @GetMapping
    @Timed(value = "products.getAll", description = "Time to get all products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        log.info("GET /api/products - Fetching all products");
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * GET PRODUCT BY ID - HOT PATH
     * Usage: Page détail produit (très fréquent)
     * Cache: Multi-level (Caffeine + Redis)
     *
     * POURQUOI HOT PATH?
     * - Requête la plus fréquente
     * - Critique pour performance
     * - Doit être ultra-rapide (<50ms)
     */
    @GetMapping("/{id}")
    @Timed(value = "products.getById", description = "Time to get product by ID")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("GET /api/products/{} - Fetching product", id);
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * GET PRODUCT BY ID WITH DISTRIBUTED LOCK
     * Usage: Alternative avec protection cache stampede
     *
     * QUAND UTILISER?
     * - Données très sollicitées
     * - Cache expire pendant pic de traffic
     * - Ex: Page produit en promotion flash
     */
    @GetMapping("/{id}/with-lock")
    @Timed(value = "products.getByIdWithLock", description = "Time to get product with lock")
    public ResponseEntity<ProductDTO> getProductByIdWithLock(@PathVariable Long id) {
        log.info("GET /api/products/{}/with-lock - Fetching with distributed lock", id);
        ProductDTO product = productService.getProductByIdWithLock(id);
        return ResponseEntity.ok(product);
    }

    /**
     * GET PRODUCTS BY CATEGORY
     * Usage: Navigation par catégorie
     * Cache: Redis 10 min
     */
    @GetMapping("/category/{category}")
    @Timed(value = "products.getByCategory", description = "Time to get products by category")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        log.info("GET /api/products/category/{} - Fetching products", category);
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    /**
     * SEARCH PRODUCTS
     * Usage: Barre de recherche
     * Cache: Redis 5 min (résultats changent)
     */
    @GetMapping("/search")
    @Timed(value = "products.search", description = "Time to search products")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        log.info("GET /api/products/search?keyword={} - Searching products", keyword);
        List<ProductDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    /**
     * GET PRODUCTS BY PRICE RANGE
     * Usage: Filtre de prix
     * Cache: Redis 3 min
     */
    @GetMapping("/price-range")
    @Timed(value = "products.getByPriceRange", description = "Time to get products by price range")
    public ResponseEntity<List<ProductDTO>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        log.info("GET /api/products/price-range?min={}&max={}", minPrice, maxPrice);
        List<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    /**
     * GET ALL CATEGORIES
     * Usage: Menu navigation
     * Cache: Redis 1 heure (très stable)
     */
    @GetMapping("/categories")
    @Timed(value = "products.getCategories", description = "Time to get all categories")
    public ResponseEntity<List<String>> getAllCategories() {
        log.info("GET /api/products/categories - Fetching all categories");
        List<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * CREATE PRODUCT
     * Usage: Admin - Ajout produit
     * Side-effect: Invalidate caches
     */
    @PostMapping
    @Timed(value = "products.create", description = "Time to create product")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        log.info("POST /api/products - Creating product: {}", productDTO.getName());
        ProductDTO created = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * UPDATE PRODUCT
     * Usage: Admin - Modification produit
     * Side-effect: Update cache
     */
    @PutMapping("/{id}")
    @Timed(value = "products.update", description = "Time to update product")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDTO productDTO) {
        log.info("PUT /api/products/{} - Updating product", id);
        ProductDTO updated = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE PRODUCT
     * Usage: Admin - Suppression
     * Side-effect: Clear all related caches
     */
    @DeleteMapping("/{id}")
    @Timed(value = "products.delete", description = "Time to delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{} - Deleting product", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * HEALTH CHECK
     * Usage: Monitoring, load balancer
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}