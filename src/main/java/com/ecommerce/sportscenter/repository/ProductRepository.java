package com.ecommerce.sportscenter.repository;

import com.ecommerce.sportscenter.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}