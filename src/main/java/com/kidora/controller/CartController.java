package com.kidora.controller;

import com.kidora.dto.cart.CartItemRequest;
import com.kidora.dto.cart.CartItemResponse;
import com.kidora.entity.CartItem;
import com.kidora.entity.User;
import com.kidora.service.CartService;
import com.kidora.service.UserService;
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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CartController {
    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getCart(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            List<CartItem> items = cartService.getCart(user);
            List<CartItemResponse> data = items.stream().map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("Error fetching cart", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> addOrUpdate(@RequestBody CartItemRequest request, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            CartItem saved = cartService.addOrUpdate(user, request);
            return ResponseEntity.ok(Map.of("success", true, "data", toResponse(saved)));
        } catch (Exception e) {
            log.error("Error adding/updating cart item", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> remove(@RequestParam Long productId, @RequestParam String selectedSize, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            cartService.remove(user, productId, selectedSize);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error removing cart item", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            cartService.clear(user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error clearing cart", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse res = new CartItemResponse();
    // Frontend expects `id` to be the product id
    res.setId(item.getProduct().getId());
    res.setProductId(item.getProduct().getId());
        res.setTitle(item.getProduct().getTitle());
        res.setImage(item.getProduct().getMainImage());
        res.setCategory(item.getProduct().getCategory());
        res.setRating(item.getProduct().getRating());
        res.setDiscount(item.getProduct().getDiscount());
    // Format price with rounding to avoid ArithmeticException
    res.setPrice("৳" + item.getProduct().getDiscountedPrice().setScale(0, RoundingMode.HALF_UP).toPlainString());
        res.setQuantity(item.getQuantity());
        res.setSelectedSize(item.getSelectedSize());
        return res;
    }
}
