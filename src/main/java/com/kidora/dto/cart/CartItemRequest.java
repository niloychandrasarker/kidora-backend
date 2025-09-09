package com.kidora.dto.cart;

import lombok.Data;

@Data
public class CartItemRequest {
    private Long productId;
    private Integer quantity = 1;
    private String selectedSize;
}
