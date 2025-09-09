package com.kidora.service;

import com.kidora.entity.Product;
import com.kidora.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }
    
    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
    }
    
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
    
    public List<Product> searchProducts(String query) {
        return productRepository.searchProducts(query);
    }
    
    public Product createProduct(Product product) {
        product.setActive(true);
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: {}", savedProduct.getTitle());
        return savedProduct;
    }
    
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        
        product.setTitle(productDetails.getTitle());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setRating(productDetails.getRating());
        product.setCategory(productDetails.getCategory());
        product.setDiscount(productDetails.getDiscount());
        product.setDescription(productDetails.getDescription());
        product.setVideoUrl(productDetails.getVideoUrl());
        product.setMainImage(productDetails.getMainImage());
        product.setImages(productDetails.getImages());
        product.setAvailableSizes(productDetails.getAvailableSizes());
        product.setUpdatedAt(LocalDateTime.now());
        
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getTitle());
        return updatedProduct;
    }
    
    // Overloaded method for direct product object update
    public Product updateProduct(Product product) {
        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getTitle());
        return updatedProduct;
    }
    
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product deactivated successfully: {}", product.getTitle());
    }
    
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findByStockLessThanAndActiveTrue(threshold);
    }
    
    public List<String> getCategories() {
        // This could be improved by creating a Category entity
        // For now, return hardcoded categories matching frontend
        return List.of("women", "men", "kids", "rich");
    }
}
