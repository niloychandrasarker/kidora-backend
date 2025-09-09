package com.kidora.controller;

import com.kidora.dto.order.OrderResponse;
import com.kidora.entity.Order;
import com.kidora.service.OrderService;
import com.kidora.service.UserService;
import com.kidora.entity.ReturnRequest;
import com.kidora.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
// Allow both ADMIN and SUB_ADMIN (SecurityConfig already permits both). Class-level guard updated accordingly.
@PreAuthorize("hasAnyRole('ADMIN','SUB_ADMIN')")
public class AdminController {
    
    private final OrderService orderService;
    private final UserService userService;
    private final ReturnRequestRepository returnRequestRepository;
    
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Long todayOrders = orderService.getTodayOrdersCount();
            Double todayRevenue = orderService.getTodayRevenue();
            Long monthlyOrders = orderService.getThisMonthOrdersCount();
            Double monthlyRevenue = orderService.getThisMonthRevenue();
            
            Map<String, Object> stats = Map.of(
                "todayOrders", todayOrders,
                "todayRevenue", todayRevenue,
                "monthlyOrders", monthlyOrders,
                "monthlyRevenue", monthlyRevenue
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
        } catch (Exception e) {
            log.error("Error fetching dashboard stats", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<?> getDashboardOverview() {
        try {
            // Core metrics
            double totalRevenue = orderService.getTotalRevenueAllTime();
            long totalOrders = orderService.getTotalOrders();
            long itemsSold7d = orderService.getItemsSoldLastNDays(7);
            var revenueTrend = orderService.getRevenueTrendLastNDays(7);
            var topProducts = orderService.getTopProducts(5);
            long returnsPending = returnRequestRepository.findAll().stream().filter(r->r.getStatus()==com.kidora.entity.ReturnRequest.Status.PENDING).count();
            long returnsApproved = returnRequestRepository.findAll().stream().filter(r->r.getStatus()==com.kidora.entity.ReturnRequest.Status.APPROVED).count();

            var data = Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "itemsSold7d", itemsSold7d,
                "revenueTrend", revenueTrend,
                "topProducts", topProducts,
                "returns", Map.of("pending", returnsPending, "approved", returnsApproved)
            );
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("Error building overview", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllOrders(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> ordersPage = orderService.getAllOrders(pageable);
            
            List<OrderResponse> orders = ordersPage.getContent().stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = Map.of(
                "orders", orders,
                "currentPage", ordersPage.getNumber(),
                "totalPages", ordersPage.getTotalPages(),
                "totalElements", ordersPage.getTotalElements(),
                "hasNext", ordersPage.hasNext(),
                "hasPrevious", ordersPage.hasPrevious()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching all orders", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/returns")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getReturnRequests() {
        try {
            var list = returnRequestRepository.findAllWithOrderAndUser();
        var resp = list.stream().map(rr -> Map.ofEntries(
            Map.entry("id", rr.getId()),
            Map.entry("orderId", rr.getOrder().getId()),
            Map.entry("orderNumber", rr.getOrder().getOrderNumber()),
            Map.entry("userEmail", rr.getUser().getEmail()),
            Map.entry("status", rr.getStatus().name()),
            Map.entry("reason", rr.getReason()),
            Map.entry("createdAt", rr.getCreatedAt()),
            Map.entry("productId", rr.getProductId()),
            Map.entry("reasonCategory", rr.getReasonCategory()),
            Map.entry("contactPhone", rr.getContactPhone()),
            Map.entry("contactEmail", rr.getContactEmail()),
            Map.entry("photos", rr.getPhotoUrls()),
            Map.entry("orderCreatedAt", rr.getOrder().getCreatedAt()),
            Map.entry("deliveredAt", rr.getOrder().getDeliveredTime())
        )).toList();
            return ResponseEntity.ok(Map.of("success", true, "data", resp));
        } catch (Exception e) {
            log.error("Error fetching return requests", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // List all users (basic fields)
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllUsers() {
        try {
            var users = userService.findAll().stream().map(u -> {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", u.getId());
                m.put("email", u.getEmail());
                m.put("role", u.getRole().name());
                m.put("firstName", u.getFirstName());
                m.put("lastName", u.getLastName());
                m.put("phone", u.getPhone());
                m.put("createdAt", u.getCreatedAt());
                return m;
            }).toList();
            return ResponseEntity.ok(Map.of("success", true, "data", users));
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestBody Map<String,String> body) {
        try {
            String roleStr = body.getOrDefault("role", "").toUpperCase();
            if (roleStr.isEmpty()) throw new IllegalArgumentException("Role required");
            // Prevent demoting full ADMIN via this endpoint (only change between USER and SUB_ADMIN)
            var user = userService.getById(id); // returns User
            if (user.getRole() == com.kidora.entity.User.Role.ADMIN && !"ADMIN".equals(roleStr)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot change main ADMIN role"));
            }
            com.kidora.entity.User.Role newRole = com.kidora.entity.User.Role.valueOf(roleStr);
            if (newRole == com.kidora.entity.User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Use primary admin account for ADMIN role"));
            }
            user.setRole(newRole);
            userService.updateUser(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Role updated", "data", Map.of(
                "id", user.getId(),
                "role", user.getRole().name()
            )));
        } catch (Exception e) {
            log.error("Error changing user role", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/returns/{id}/status")
    public ResponseEntity<?> updateReturnStatus(@PathVariable Long id, @RequestBody Map<String,String> body) {
        try {
            String st = body.getOrDefault("status", "");
            ReturnRequest.Status status = ReturnRequest.Status.valueOf(st.toUpperCase());
            var updated = orderService.updateReturnStatus(id, status);
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                    "id", updated.getId(),
                    "status", updated.getStatus().name(),
                    "completedAt", updated.getCompletedAt()
            )));
        } catch (Exception e) {
            log.error("Error updating return status", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            
            List<OrderResponse> response = orders.stream()
                    .map(this::convertToOrderResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching orders by status", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId,
                                             @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(statusStr.toUpperCase());
            // Business rule: once delivered, cannot cancel.
            if (newStatus == Order.OrderStatus.CANCELLED) {
                Order current = orderService.getOrderById(orderId);
                if (current.getStatus() == Order.OrderStatus.DELIVERED) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Delivered order cannot be cancelled"
                    ));
                }
            }
            
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            OrderResponse response = convertToOrderResponse(updatedOrder);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Order status updated successfully",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error updating order status", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PutMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long orderId,
                                                @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("paymentStatus");
            Order.PaymentStatus paymentStatus = Order.PaymentStatus.valueOf(statusStr.toUpperCase());
            
            Order updatedOrder = orderService.updatePaymentStatus(orderId, paymentStatus);
            OrderResponse response = convertToOrderResponse(updatedOrder);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment status updated successfully",
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error updating payment status", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            OrderResponse response = convertToOrderResponse(order);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error fetching order details", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    private OrderResponse convertToOrderResponse(Order order) {
        // Same implementation as in OrderController
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
        
    // Set order items
        if (order.getOrderItems() != null) {
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
        }

    // Include return info if exists
    var existing = orderService.getReturnByOrder(order);
    boolean eligible = order.getStatus() == Order.OrderStatus.DELIVERED && order.getDeliveredTime() != null
        && !order.getDeliveredTime().isBefore(java.time.LocalDateTime.now().minusDays(3));
    response.setReturnEligible(eligible);
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
}
