package com.ecommerce.sportscenter.service;

import com.ecommerce.sportscenter.entity.Product;
import com.ecommerce.sportscenter.model.ProductResponse;
import com.ecommerce.sportscenter.repository.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Override
    public ProductResponse getProductById(Integer productId) {
        log.info("Fetching Product by Id: {}", productId);
        Optional<Product> product = Optional.ofNullable(productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Prodcut with Id: {} not found")));
         ProductResponse productResponse = convertToProductResponse(product.get());

         log.info("Fetching Product by Id: {}", productId);
        return productResponse;
    }

    @Override
    public Page<ProductResponse> getProducts(Pageable pageable, Integer brandId, Integer typeId, String keyword) {
        log.info("Fetching all products");
        Specification<Product> spec = Specification.where(null);
        if(brandId!=null){
            spec = spec.and((root, query, criteriaBuilder)-> criteriaBuilder.equal(root.get("brand").get("id"), brandId));
        }
        Page<Product> productPage = productRepository.findAll(pageable);
        Page<ProductResponse> productResponses = productPage
                .map(this::convertToProductResponse);
        log.info("Products fetched");
        return productResponses;
    }

    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .pictureUrl(product.getPictureUrl())
                .productBrand(product.getBrand().getName())
                .productType(product.getType().getName())
                .build();
    }

}
