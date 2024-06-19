package com.ecommerce.sportscenter.repository;

import com.ecommerce.sportscenter.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
}