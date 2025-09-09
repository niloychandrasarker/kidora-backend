package com.kidora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hero_banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeroBanner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to product
    private Long productId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;      // optional override for display
    private BigDecimal oldPrice;   // optional
    private Integer discount = 0;  // percentage

    @Column(nullable = false)
    private String imageUrl;

    private Double rating = 0.0;
    private String reviews; // e.g., "1000+"
    private String category; // men/women/kids etc

    @ElementCollection
    @CollectionTable(name = "hero_banner_features", joinColumns = @JoinColumn(name = "banner_id"))
    @Column(name = "feature")
    private List<String> features;

    private Boolean active = true;
    private Integer orderIndex = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
