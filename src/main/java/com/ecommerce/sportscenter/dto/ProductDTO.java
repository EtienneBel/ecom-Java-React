package com.ecommerce.sportscenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProductDTO - Data Transfer Object
 *
 * WHY DTO? Séparer l'entité JPA des réponses API
 * - Évite d'exposer la structure DB
 * - Plus léger (pas de relations JPA)
 * - Facilite la sérialisation Redis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String brand;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Metadata pour monitoring
    private String cacheSource; // "L1_CAFFEINE", "L2_REDIS", "DATABASE"
}
