package com.kidora.dto.cart;

import lombok.Data;

@Data
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String title;
    private String image;
    private String category;
    private Double rating;
    private Integer discount;
    private String price; // string to align with frontend formatting e.g., "৳1200"
    private Integer quantity;
    private String selectedSize;
}
