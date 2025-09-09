package com.kidora.controller;

import com.kidora.dto.wishlist.WishlistItemResponse;
import com.kidora.entity.User;
import com.kidora.entity.WishlistItem;
import com.kidora.service.UserService;
import com.kidora.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WishlistController {
    private final WishlistService wishlistService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getWishlist(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            List<WishlistItem> items = wishlistService.getWishlist(user);
            List<WishlistItemResponse> data = items.stream().map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("Error fetching wishlist", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestParam Long productId, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            WishlistItem changed = wishlistService.toggle(user, productId);
            return ResponseEntity.ok(Map.of("success", true, "data", toResponse(changed)));
        } catch (Exception e) {
            log.error("Error toggling wishlist", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@RequestParam Long productId, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            wishlistService.remove(user, productId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error removing wishlist item", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        WishlistItemResponse res = new WishlistItemResponse();
    // Frontend expects `id` to be the product id
    res.setId(item.getProduct().getId());
        res.setProductId(item.getProduct().getId());
        res.setTitle(item.getProduct().getTitle());
        res.setImage(item.getProduct().getMainImage());
        res.setCategory(item.getProduct().getCategory());
        res.setRating(item.getProduct().getRating());
        res.setDiscount(item.getProduct().getDiscount());
    res.setPrice("৳" + item.getProduct().getDiscountedPrice().setScale(0, RoundingMode.HALF_UP).toPlainString());
        return res;
    }
}
