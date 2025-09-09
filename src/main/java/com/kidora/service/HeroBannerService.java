package com.kidora.service;

import com.kidora.entity.HeroBanner;
import com.kidora.repository.HeroBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HeroBannerService {
    private final HeroBannerRepository repo;

    public List<HeroBanner> getActive() {
        return repo.findByActiveTrueOrderByOrderIndexAscCreatedAtDesc();
    }

    public HeroBanner create(HeroBanner b) {
        b.setId(null);
        b.setActive(true);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        return repo.save(b);
    }

    public HeroBanner update(Long id, HeroBanner incoming) {
        HeroBanner b = repo.findById(id).orElseThrow(() -> new RuntimeException("Banner not found"));
    if (incoming.getTitle() != null) b.setTitle(incoming.getTitle());
    if (incoming.getDescription() != null) b.setDescription(incoming.getDescription());
    if (incoming.getPrice() != null) b.setPrice(incoming.getPrice());
    if (incoming.getOldPrice() != null) b.setOldPrice(incoming.getOldPrice());
    if (incoming.getDiscount() != null) b.setDiscount(incoming.getDiscount());
    if (incoming.getImageUrl() != null) b.setImageUrl(incoming.getImageUrl());
    if (incoming.getRating() != null) b.setRating(incoming.getRating());
    if (incoming.getReviews() != null) b.setReviews(incoming.getReviews());
    if (incoming.getCategory() != null) b.setCategory(incoming.getCategory());
    if (incoming.getFeatures() != null) b.setFeatures(incoming.getFeatures());
    if (incoming.getProductId() != null) b.setProductId(incoming.getProductId());
    if (incoming.getOrderIndex() != null) b.setOrderIndex(incoming.getOrderIndex());
    b.setActive(incoming.getActive() != null ? incoming.getActive() : b.getActive());
        b.setUpdatedAt(LocalDateTime.now());
        return repo.save(b);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
