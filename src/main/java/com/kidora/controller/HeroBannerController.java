package com.kidora.controller;

import com.kidora.entity.HeroBanner;
import com.kidora.service.HeroBannerService;
import com.kidora.service.LocalObjectStorageService;
import com.kidora.service.ProductService;
import com.kidora.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hero-banners")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HeroBannerController {
    private final HeroBannerService service;
    private final LocalObjectStorageService localStorage;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> list() {
        List<HeroBanner> banners = service.getActive();
        return ResponseEntity.ok(Map.of("success", true, "data", banners));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) BigDecimal oldPrice,
            @RequestParam(required = false, defaultValue = "0") Integer discount,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) String reviews,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) List<String> features,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages,
            @RequestParam(required = false, defaultValue = "0") Integer orderIndex
    ) {
        String stored = localStorage.storeFile(image);
        String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/products/")
                .path(stored)
                .toUriString();

        HeroBanner b = new HeroBanner();
        b.setTitle(title);
        b.setDescription(description);
        b.setPrice(price);
        b.setOldPrice(oldPrice);
        b.setDiscount(discount);
        b.setRating(rating != null ? Math.max(0.0, Math.min(5.0, rating)) : 0.0);
        b.setReviews(reviews);
        b.setCategory(category);
        b.setFeatures(features);
        b.setImageUrl(imageUrl);
        b.setOrderIndex(orderIndex);
        // Auto-create product if not linked
        if (productId == null) {
            Product p = new Product();
            p.setTitle(title);
            p.setPrice(price != null ? price : java.math.BigDecimal.ZERO);
            p.setStock(100);
            p.setRating(rating != null ? Math.max(0.0, Math.min(5.0, rating)) : 0.0);
            p.setCategory(category != null ? category : "kids");
            p.setDiscount(discount != null ? discount : 0);
            p.setDescription(description);
            p.setMainImage(imageUrl);
            java.util.List<String> gallery = new java.util.ArrayList<>();
            gallery.add(imageUrl);
            // Store and add any additional images
            if (additionalImages != null && additionalImages.length > 0) {
                for (MultipartFile f : additionalImages) {
                    if (f != null && !f.isEmpty()) {
                        String storedExtra = localStorage.storeFile(f);
                        String urlExtra = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/uploads/products/")
                                .path(storedExtra)
                                .toUriString();
                        if (!gallery.contains(urlExtra)) gallery.add(urlExtra);
                    }
                }
            }
            p.setImages(gallery);
            p.setAvailableSizes(java.util.List.of("XS","S","M","L","XL","XXL"));
            Product created = productService.createProduct(p);
            b.setProductId(created.getId());
        } else {
            b.setProductId(productId);
        }

        HeroBanner saved = service.create(b);

        // If linked product exists and additional images uploaded, append them to product's gallery
        if (saved.getProductId() != null && additionalImages != null && additionalImages.length > 0) {
            var product = productService.getProductById(saved.getProductId());
            var gallery = product.getImages() != null ? new java.util.ArrayList<>(product.getImages()) : new java.util.ArrayList<String>();
            for (MultipartFile f : additionalImages) {
                if (f != null && !f.isEmpty()) {
                    String storedExtra = localStorage.storeFile(f);
                    String urlExtra = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/products/")
                            .path(storedExtra)
                            .toUriString();
                    if (!gallery.contains(urlExtra)) gallery.add(urlExtra);
                }
            }
            product.setImages(gallery);
            productService.updateProduct(product);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) BigDecimal oldPrice,
            @RequestParam(required = false, defaultValue = "0") Integer discount,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) String reviews,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) List<String> features,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages,
            @RequestParam(required = false, defaultValue = "0") Integer orderIndex
    ) {
        HeroBanner b = new HeroBanner();
        b.setTitle(title);
        b.setDescription(description);
        b.setPrice(price);
        b.setOldPrice(oldPrice);
        b.setDiscount(discount);
        b.setRating(rating != null ? Math.max(0.0, Math.min(5.0, rating)) : 0.0);
        b.setReviews(reviews);
        b.setCategory(category);
        b.setProductId(productId);
        b.setFeatures(features);
        b.setOrderIndex(orderIndex);

        if (image != null && !image.isEmpty()) {
            String stored = localStorage.storeFile(image);
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/products/")
                    .path(stored)
                    .toUriString();
            b.setImageUrl(imageUrl);
        }

        var saved = service.update(id, b);

        // Append any newly uploaded additional images to linked product
        if (productId != null && additionalImages != null && additionalImages.length > 0) {
            var product = productService.getProductById(productId);
            var gallery = product.getImages() != null ? new java.util.ArrayList<>(product.getImages()) : new java.util.ArrayList<String>();
            for (MultipartFile f : additionalImages) {
                if (f != null && !f.isEmpty()) {
                    String storedExtra = localStorage.storeFile(f);
                    String urlExtra = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/products/")
                            .path(storedExtra)
                            .toUriString();
                    if (!gallery.contains(urlExtra)) gallery.add(urlExtra);
                }
            }
            product.setImages(gallery);
            productService.updateProduct(product);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
