package com.kidora.service;

import com.kidora.entity.Product;
import com.kidora.entity.User;
import com.kidora.entity.WishlistItem;
import com.kidora.repository.ProductRepository;
import com.kidora.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public List<WishlistItem> getWishlist(User user) {
        return wishlistItemRepository.findByUser(user);
    }

    @Transactional
    public WishlistItem toggle(User user, Long productId) {
        var existing = wishlistItemRepository.findByUserAndProduct_Id(user, productId).orElse(null);
        if (existing != null) {
            wishlistItemRepository.delete(existing);
            return existing;
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        return wishlistItemRepository.save(item);
    }

    @Transactional
    public void remove(User user, Long productId) {
        wishlistItemRepository.deleteByUserAndProduct_Id(user, productId);
    }
}
