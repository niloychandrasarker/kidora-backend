package com.kidora.repository;

import com.kidora.entity.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long> {
    List<HeroBanner> findByActiveTrueOrderByOrderIndexAscCreatedAtDesc();
}
