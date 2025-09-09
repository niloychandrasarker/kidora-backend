package com.kidora.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;
    
    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "COD|ONLINE", message = "Payment method must be COD or ONLINE")
    private String paymentMethod;
    
    private String paymentProvider; // bkash, nagad, rocket
    private String senderNumber;
    private String transactionId;
    
    @NotBlank(message = "Shipping name is required")
    private String shippingName;
    
    @NotBlank(message = "Shipping phone is required")
    private String shippingPhone;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "Shipping city is required")
    private String shippingCity;
    
    @NotBlank(message = "Postal code is required")
    private String shippingPostalCode;
    
    private String shippingNotes;
    
    @Data
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        
        @NotBlank(message = "Selected size is required")
        private String selectedSize;
    }
}
