package com.kidora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stock;
    
    private Double rating = 0.0;
    
    @Column(nullable = false)
    private String category;
    
    private Integer discount = 0;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String videoUrl;
    
    @Column(nullable = false)
    private String mainImage;
    
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images;
    
    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size")
    private List<String> availableSizes;
    
    private boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public BigDecimal getDiscountedPrice() {
        if (discount != null && discount > 0) {
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100));
            return price.subtract(discountAmount);
        }
        return price;
    }
}
