package com.kidora.dto.wishlist;

import lombok.Data;

@Data
public class WishlistItemResponse {
    private Long id;
    private Long productId;
    private String title;
    private String image;
    private String category;
    private Double rating;
    private Integer discount;
    private String price;
}
