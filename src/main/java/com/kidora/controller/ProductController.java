package com.kidora.controller;

import com.kidora.entity.Product;
import com.kidora.service.ProductService;
import com.kidora.service.FileUploadService;
import com.kidora.service.LocalObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductController {
    
    private final ProductService productService;
    private final FileUploadService fileUploadService;
    private final LocalObjectStorageService localObjectStorageService;
    
    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String search) {
        try {
            if (search != null && !search.trim().isEmpty()) {
                List<Product> products = productService.searchProducts(search);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", products
                ));
            }
            
            if (category != null && !category.trim().isEmpty()) {
                List<Product> products = productService.getProductsByCategory(category);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", products
                ));
            }
            
            if (page >= 0 && size > 0) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Product> productsPage = productService.getActiveProducts(pageable);
                
                Map<String, Object> response = Map.of(
                    "products", productsPage.getContent(),
                    "currentPage", productsPage.getNumber(),
                    "totalPages", productsPage.getTotalPages(),
                    "totalElements", productsPage.getTotalElements(),
                    "hasNext", productsPage.hasNext(),
                    "hasPrevious", productsPage.hasPrevious()
                );
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
                ));
            }
            
            List<Product> products = productService.getAllActiveProducts();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", products
            ));
        } catch (Exception e) {
            log.error("Error fetching products", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", product
            ));
        } catch (Exception e) {
            log.error("Error fetching product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            List<String> categories = productService.getCategories();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", categories
            ));
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product savedProduct = productService.createProduct(product);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product created successfully",
                "data", savedProduct
            ));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        try {
            Product updatedProduct = productService.updateProduct(id, product);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product updated successfully",
                "data", updatedProduct
            ));
        } catch (Exception e) {
            log.error("Error updating product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
        try {
            List<Product> products = productService.getLowStockProducts(threshold);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", products
            ));
        } catch (Exception e) {
            log.error("Error fetching low stock products", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // Admin Product Management with File Upload
    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> createProduct(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("category") String category,
            @RequestParam(value = "discount", defaultValue = "0") Integer discount,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "availableSizes", required = false) List<String> availableSizes,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages) {
        try {
            // Upload main image
            // Upload main image using local storage service
        String mainStored = localObjectStorageService.storeFile(mainImage);
        String mainImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/products/")
            .path(mainStored)
            .toUriString();
            
            // Upload additional images
            List<String> imageUrls = new ArrayList<>();
            imageUrls.add(mainImageUrl); // Add main image to list
            
            if (additionalImages != null && additionalImages.length > 0) {
                for (MultipartFile f : additionalImages) {
                    if (f != null && !f.isEmpty()) {
            String stored = localObjectStorageService.storeFile(f);
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/products/")
                .path(stored)
                .toUriString();
            imageUrls.add(url);
                    }
                }
            }
            
            // Create product
            Product product = new Product();
            product.setTitle(title);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(stock);
            product.setCategory(category);
            product.setDiscount(discount);
            product.setVideoUrl(videoUrl);
            product.setMainImage(mainImageUrl);
            product.setImages(imageUrls);
            if (availableSizes != null) {
                product.setAvailableSizes(availableSizes);
            }
            if (rating == null) {
                product.setRating(0.0);
            } else {
                // clamp rating to [0,5]
                double r = Math.max(0.0, Math.min(5.0, rating));
                product.setRating(r);
            }
            
            Product savedProduct = productService.createProduct(product);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product created successfully",
                "data", savedProduct
            ));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("category") String category,
            @RequestParam(value = "discount", defaultValue = "0") Integer discount,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "availableSizes", required = false) List<String> availableSizes,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "additionalImages", required = false) MultipartFile[] additionalImages,
            @RequestParam(value = "existingImages", required = false) List<String> existingImages) {
        try {
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Product not found"
                ));
            }
            
            // Update basic info
            existingProduct.setTitle(title);
            existingProduct.setDescription(description);
            existingProduct.setPrice(price);
            existingProduct.setStock(stock);
            existingProduct.setCategory(category);
            existingProduct.setDiscount(discount);
            existingProduct.setVideoUrl(videoUrl);
            if (rating != null) {
                double r = Math.max(0.0, Math.min(5.0, rating));
                existingProduct.setRating(r);
            }
            if (availableSizes != null) {
                // Allow clearing sizes by sending empty list
                existingProduct.setAvailableSizes(availableSizes);
            }
            
            // Update main image if provided
            if (mainImage != null && !mainImage.isEmpty()) {
                // Delete old main image
                fileUploadService.deleteFile(existingProduct.getMainImage());
                
                // Upload new main image
                // Upload new main image via local storage service
        String newStored = localObjectStorageService.storeFile(mainImage);
        String newMainImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/products/")
            .path(newStored)
            .toUriString();
                existingProduct.setMainImage(newMainImageUrl);
            }

            // Rebuild full images list: main + kept existing + newly uploaded
            List<String> rebuilt = new ArrayList<>();
            String mainUrl = existingProduct.getMainImage();
            if (mainUrl != null) rebuilt.add(mainUrl);
            if (existingImages != null && !existingImages.isEmpty()) {
                for (String url : existingImages) {
                    if (url != null && !url.isBlank() && !url.equals(mainUrl)) {
                        rebuilt.add(url);
                    }
                }
            } else if (existingProduct.getImages() != null && existingProduct.getImages().size() > 1) {
                for (int i = 1; i < existingProduct.getImages().size(); i++) {
                    String url = existingProduct.getImages().get(i);
                    if (url != null && !url.equals(mainUrl)) rebuilt.add(url);
                }
            }
            if (additionalImages != null && additionalImages.length > 0) {
                for (MultipartFile f : additionalImages) {
                    if (f != null && !f.isEmpty()) {
                        String stored = localObjectStorageService.storeFile(f);
                        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/products/")
                            .path(stored)
                            .toUriString();
                        rebuilt.add(url);
                    }
                }
            }
            existingProduct.setImages(rebuilt);
            
            Product updatedProduct = productService.updateProduct(existingProduct);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product updated successfully",
                "data", updatedProduct
            ));
        } catch (Exception e) {
            log.error("Error updating product", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // File Upload endpoint for testing
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileUploadService.uploadSingleFile(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileUrl", fileUrl
            ));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
