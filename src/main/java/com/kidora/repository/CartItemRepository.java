package com.kidora.repository;

import com.kidora.entity.CartItem;
import com.kidora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct_IdAndSelectedSize(User user, Long productId, String selectedSize);
    void deleteByUserAndProduct_IdAndSelectedSize(User user, Long productId, String selectedSize);
}
