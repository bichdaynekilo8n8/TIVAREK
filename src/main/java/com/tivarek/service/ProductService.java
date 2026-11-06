package com.tivarek.service;
import java.util.List;
import com.tivarek.dto.ProductCardDto;
import com.tivarek.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProductService {
    private final ProductRepository repository;
    public ProductService(ProductRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public List<ProductCardDto> getFeaturedProducts() { return repository.findTop6ByActiveTrueOrderByIdDesc(); }
}
