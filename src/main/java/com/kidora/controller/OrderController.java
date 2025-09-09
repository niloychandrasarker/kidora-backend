package com.kidora.controller;

import com.kidora.dto.order.CreateOrderRequest;
import com.kidora.dto.order.OrderResponse;
import com.kidora.entity.*;
import com.kidora.repository.ProductRepository;
import com.kidora.entity.ReturnRequest;
import com.kidora.service.LocalObjectStorageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.kidora.service.OrderService;
import com.kidora.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final LocalObjectStorageService localObjectStorageService;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                       Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            
            // Create order items
            List<OrderItem> orderItems = request.getItems().stream().map(itemReq -> {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));
                
                if (product.getStock() < itemReq.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getTitle());
                }
                
                BigDecimal unitPrice = product.getDiscountedPrice();
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setSelectedSize(itemReq.getSelectedSize());
                orderItem.setUnitPrice(unitPrice);
                orderItem.setTotalPrice(totalPrice);
                orderItem.setProductTitle(product.getTitle());
                orderItem.setProductImage(product.getMainImage());
                
                return orderItem;
            }).collect(Collectors.toList());
            
            Order.PaymentMethod paymentMethod = Order.PaymentMethod.valueOf(request.getPaymentMethod());
            
            Order order = orderService.createOrder(
                user, orderItems, paymentMethod, request.getPaymentProvider(),
                request.getSenderNumber(), request.getTransactionId(),
                request.getShippingName(), request.getShippingPhone(),
                request.getShippingAddress(), request.getShippingCity(),
                request.getShippingPostalCode(), request.getShippingNotes()
            );
            
            OrderResponse response = convertToOrderResponse(order);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Order placed successfully",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> requestReturn(@PathVariable Long orderId,
                                           @RequestParam(value = "reason", required = false) String reason,
                                           @RequestParam(value = "productId", required = false) Long productId,
                                           @RequestParam(value = "phone", required = false) String phone,
                                           @RequestParam(value = "email", required = false) String email,
                                           @RequestParam(value = "reasonCategory", required = false) String reasonCategory,
                                           @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                                           Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            reason = String.valueOf(reason == null ? "" : reason).trim();
            if (reason.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Reason is required"
                ));
            }
            java.util.List<String> photoUrls = new java.util.ArrayList<>();
            if (photos != null) {
                for (MultipartFile f : photos) {
                    if (f != null && !f.isEmpty()) {
                        String stored = localObjectStorageService.storeFile(f);
                        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/uploads/products/")
                                .path(stored)
                                .toUriString();
                        photoUrls.add(url);
                    }
                }
            }
            orderService.createReturnRequest(user, orderId, reason, productId, phone, email, reasonCategory, photoUrls);
            Order order = orderService.getOrderById(orderId);
            OrderResponse resp = convertToOrderResponse(order);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Return request submitted",
                    "data", resp
            ));
        } catch (Exception e) {
            log.error("Error submitting return request", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            List<Order> orders = orderService.getUserOrders(user);
            
            List<OrderResponse> response = orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching user orders", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId,
                                        Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            Order order = orderService.getOrderById(orderId);
            
            // Check if user owns this order or is admin
            if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Unauthorized to view this order"
                ));
            }
            
            OrderResponse response = convertToOrderResponse(order);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching order", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            User user = (User) userService.loadUserByUsername(authentication.getName());
            Order order = orderService.getOrderById(orderId);
            if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Unauthorized to cancel this order"
                ));
            }
            if (order.getStatus().ordinal() >= Order.OrderStatus.SHIPPED.ordinal()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Order cannot be cancelled after it has been shipped"
                ));
            }
            Order cancelled = orderService.updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
            OrderResponse response = convertToOrderResponse(cancelled);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order cancelled",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("Error cancelling order", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    private OrderResponse convertToOrderResponse(Order order) {
    OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setSubtotal(order.getSubtotal());
        response.setShippingCost(order.getShippingCost());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setPaymentMethod(order.getPaymentMethod().name());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setPaymentProvider(order.getPaymentProvider());
        response.setSenderNumber(order.getSenderNumber());
        response.setTransactionId(order.getTransactionId());
        
        response.setShippingName(order.getShippingName());
        response.setShippingPhone(order.getShippingPhone());
        response.setShippingAddress(order.getShippingAddress());
        response.setShippingCity(order.getShippingCity());
        response.setShippingPostalCode(order.getShippingPostalCode());
        response.setShippingNotes(order.getShippingNotes());
        
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        
        // Set tracking info
        OrderResponse.TrackingInfo tracking = new OrderResponse.TrackingInfo();
        tracking.setProcessingTime(order.getProcessingTime());
        tracking.setPackedTime(order.getPackedTime());
        tracking.setShippedTime(order.getShippedTime());
        tracking.setOutForDeliveryTime(order.getOutForDeliveryTime());
        tracking.setDeliveredTime(order.getDeliveredTime());
        
        // Create tracking steps
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<OrderResponse.TrackingStep> steps = List.of(
            createTrackingStep("processing", "Processing", order.getProcessingTime(), order.getStatus(), timeFormatter),
            createTrackingStep("packed", "Packed", order.getPackedTime(), order.getStatus(), timeFormatter),
            createTrackingStep("shipped", "Shipped", order.getShippedTime(), order.getStatus(), timeFormatter),
            createTrackingStep("out_for_delivery", "Out for Delivery", order.getOutForDeliveryTime(), order.getStatus(), timeFormatter),
            createTrackingStep("delivered", "Delivered", order.getDeliveredTime(), order.getStatus(), timeFormatter)
        );
        tracking.setSteps(steps);
        response.setTracking(tracking);
        
    // Set order items
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderResponse.OrderItemResponse itemResponse = new OrderResponse.OrderItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductTitle(item.getProductTitle());
                    itemResponse.setProductImage(item.getProductImage());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setSelectedSize(item.getSelectedSize());
                    itemResponse.setUnitPrice(item.getUnitPrice());
                    itemResponse.setTotalPrice(item.getTotalPrice());
                    return itemResponse;
                })
                .collect(Collectors.toList());
        response.setItems(items);

    // Return info
    boolean eligible = order.getStatus() == Order.OrderStatus.DELIVERED && order.getDeliveredTime() != null
        && !order.getDeliveredTime().isBefore(java.time.LocalDateTime.now().minusDays(3));
    response.setReturnEligible(eligible);
    ReturnRequest existing = orderService.getReturnByOrder(order);
    if (existing != null) {
        OrderResponse.ReturnInfo info = new OrderResponse.ReturnInfo();
        info.setId(existing.getId());
        info.setStatus(existing.getStatus().name());
        info.setReason(existing.getReason());
        info.setCreatedAt(existing.getCreatedAt());
            info.setProductId(existing.getProductId());
            info.setReasonCategory(existing.getReasonCategory());
            info.setContactPhone(existing.getContactPhone());
            info.setContactEmail(existing.getContactEmail());
            info.setPhotos(existing.getPhotoUrls());
        response.setReturnRequest(info);
    }
        
        return response;
    }
    
    private OrderResponse.TrackingStep createTrackingStep(String key, String label, 
                                                         java.time.LocalDateTime time, 
                                                         Order.OrderStatus currentStatus,
                                                         DateTimeFormatter timeFormatter) {
        OrderResponse.TrackingStep step = new OrderResponse.TrackingStep();
        step.setKey(key);
        step.setLabel(label);
        step.setTime(time != null ? time.format(timeFormatter) : null);
        
        // Determine if step is completed based on current status
        step.setCompleted(isStepCompleted(key, currentStatus));
        
        return step;
    }
    
    private boolean isStepCompleted(String stepKey, Order.OrderStatus currentStatus) {
        return switch (stepKey) {
            case "processing" -> true; // Always completed if order exists
            case "packed" -> currentStatus.ordinal() >= Order.OrderStatus.PACKED.ordinal();
            case "shipped" -> currentStatus.ordinal() >= Order.OrderStatus.SHIPPED.ordinal();
            case "out_for_delivery" -> currentStatus.ordinal() >= Order.OrderStatus.OUT_FOR_DELIVERY.ordinal();
            case "delivered" -> currentStatus == Order.OrderStatus.DELIVERED;
            default -> false;
        };
    }
}
