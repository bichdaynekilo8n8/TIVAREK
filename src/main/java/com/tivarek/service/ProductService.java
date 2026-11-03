package com.tivarek.service;

import com.tivarek.dto.ProductCardDto;
import com.tivarek.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(
            ProductRepository productRepository
    ) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getFeaturedProducts() {

        return productRepository
                .findTop6ByActiveTrueOrderByIdDesc()
                .stream()
                .map(product -> new ProductCardDto(
                        product.getId(),
                        product.getName(),
                        product.getPrice()
                ))
                .toList();
    }
}