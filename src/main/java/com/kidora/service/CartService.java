package com.kidora.service;

import com.kidora.dto.cart.CartItemRequest;
import com.kidora.entity.CartItem;
import com.kidora.entity.Product;
import com.kidora.entity.User;
import com.kidora.repository.CartItemRepository;
import com.kidora.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public List<CartItem> getCart(User user) {
        return cartItemRepository.findByUser(user);
    }

    @Transactional
    public CartItem addOrUpdate(User user, CartItemRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String size = req.getSelectedSize();
        if (size == null || size.isBlank()) size = "M"; // default

        CartItem item = cartItemRepository.findByUserAndProduct_IdAndSelectedSize(user, product.getId(), size)
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setSelectedSize(size);
            item.setQuantity(Math.max(1, req.getQuantity() == null ? 1 : req.getQuantity()));
        } else {
            int q = req.getQuantity() == null ? 1 : req.getQuantity();
            item.setQuantity(Math.max(1, q));
        }
        return cartItemRepository.save(item);
    }

    @Transactional
    public void remove(User user, Long productId, String selectedSize) {
        cartItemRepository.deleteByUserAndProduct_IdAndSelectedSize(user, productId, selectedSize);
    }

    @Transactional
    public void clear(User user) {
        getCart(user).forEach(ci -> cartItemRepository.deleteById(ci.getId()));
    }
}
