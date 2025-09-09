package com.kidora.repository;

import com.kidora.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByActiveTrue();
    
    List<Product> findByCategoryAndActiveTrue(String category);
    
    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("query") String query);
    
    List<Product> findByStockLessThanAndActiveTrue(Integer stockThreshold);
}
