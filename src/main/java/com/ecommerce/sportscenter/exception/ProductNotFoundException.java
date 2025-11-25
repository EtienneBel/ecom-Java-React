package com.ecommerce.sportscenter.exception;

/**
 * Exception thrown when a product is not found in the database.
 * Results in HTTP 404 response.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product not found: " + productId);
        this.productId = productId;
    }

    public ProductNotFoundException(String message) {
        super(message);
        this.productId = null;
    }

    public Long getProductId() {
        return productId;
    }
}
