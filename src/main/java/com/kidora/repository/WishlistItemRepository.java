package com.kidora.repository;

import com.kidora.entity.User;
import com.kidora.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser(User user);
    Optional<WishlistItem> findByUserAndProduct_Id(User user, Long productId);
    void deleteByUserAndProduct_Id(User user, Long productId);
}
