package com.kidora.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentProvider;
    private String senderNumber;
    private String transactionId;
    
    // Shipping details
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingNotes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Tracking timestamps
    private TrackingInfo tracking;
    
    private List<OrderItemResponse> items;

    // Return info
    private boolean returnEligible;
    private ReturnInfo returnRequest;
    
    @Data
    public static class TrackingInfo {
        private LocalDateTime processingTime;
        private LocalDateTime packedTime;
        private LocalDateTime shippedTime;
        private LocalDateTime outForDeliveryTime;
        private LocalDateTime deliveredTime;
        private List<TrackingStep> steps;
    }
    
    @Data
    public static class TrackingStep {
        private String key;
        private String label;
        private String time;
        private boolean completed;
    }
    
    @Data
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productTitle;
        private String productImage;
        private Integer quantity;
        private String selectedSize;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    public static class ReturnInfo {
        private Long id;
        private String status; // PENDING / APPROVED / REJECTED
        private String reason;
        private LocalDateTime createdAt;
    private Long productId;
    private String reasonCategory;
    private String contactPhone;
    private String contactEmail;
    private java.util.List<String> photos;
    }
}
