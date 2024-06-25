package com.ecommerce.sportscenter.controller;

import com.ecommerce.sportscenter.model.ProductResponse;
import com.ecommerce.sportscenter.service.BrandService;
import com.ecommerce.sportscenter.service.ProductService;
import com.ecommerce.sportscenter.service.TypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private TypeService typeService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id")  Integer productId){
        ProductResponse productResponse = productService.getProductById(productId);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @PageableDefault(size = 10)Pageable pageable, Integer brandId, Integer typeId, String keyword
            ){
        Page<ProductResponse> productResponses = productService.getProducts(pageable);
        return new ResponseEntity<>(productResponses, HttpStatus.OK);
    }
}
