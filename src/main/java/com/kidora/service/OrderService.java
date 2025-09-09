package com.kidora.service;

import com.kidora.entity.*;
import com.kidora.repository.OrderRepository;
import com.kidora.repository.ProductRepository;
import com.kidora.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReturnRequestRepository returnRequestRepository;
    
    public Order createOrder(User user, List<OrderItem> items, 
                           Order.PaymentMethod paymentMethod, String paymentProvider,
                           String senderNumber, String transactionId,
                           String shippingName, String shippingPhone, 
                           String shippingAddress, String shippingCity,
                           String shippingPostalCode, String shippingNotes) {
        
        // Calculate totals
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
    // Shipping: inside Dhaka = 100, outside Dhaka = 160 (no free shipping)
    boolean insideDhaka = shippingCity != null && shippingCity.trim().equalsIgnoreCase("Dhaka");
    BigDecimal shippingCost = insideDhaka ? BigDecimal.valueOf(100) : BigDecimal.valueOf(160);
        
        BigDecimal totalAmount = subtotal.add(shippingCost);
        
        // Generate order number
        String orderNumber = "ORD-" + System.currentTimeMillis();
        
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PROCESSING);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentProvider(paymentProvider);
        order.setSenderNumber(senderNumber);
        order.setTransactionId(transactionId);
        order.setProcessingTime(LocalDateTime.now());
        
        // Set payment status
        if (paymentMethod == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.PENDING_VERIFICATION);
        }
        
        // Set shipping details
        order.setShippingName(shippingName);
        order.setShippingPhone(shippingPhone);
        order.setShippingAddress(shippingAddress);
        order.setShippingCity(shippingCity);
        order.setShippingPostalCode(shippingPostalCode);
        order.setShippingNotes(shippingNotes);
        
        // Save order first to get ID
        order = orderRepository.save(order);
        
        // Set order reference in items and save
        final Order finalOrder = order;
        items.forEach(item -> item.setOrder(finalOrder));
        order.setOrderItems(items);

    // NOTE: Stock will now be deducted only when the order is marked as DELIVERED.
    // This avoids reducing stock at order placement time and aligns with the requirement.

        log.info("Order created successfully: {} for user: {}", orderNumber, user.getEmail());
        return orderRepository.save(order);
    }
    
    // Deduct stock for all items in the order
    private void updateProductStock(List<OrderItem> items) {
        items.forEach(item -> {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
        // Prevent negative stock; log a warning if this happens.
        if (newStock < 0) {
        log.warn("Stock underflow while deducting for product {} (current: {}, qty: {}). Clamping to 0.",
            product.getTitle(), product.getStock(), item.getQuantity());
        newStock = 0;
        }
            product.setStock(newStock);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        });
    }
    
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.OrderStatus previousStatus = order.getStatus();
        // Prevent cancelling a delivered order (idempotent if already cancelled earlier)
        if (previousStatus == Order.OrderStatus.DELIVERED && newStatus == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Delivered order cannot be cancelled");
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        // Set timestamp for status
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case PROCESSING -> order.setProcessingTime(now);
            case PACKED -> order.setPackedTime(now);
            case SHIPPED -> order.setShippedTime(now);
            case OUT_FOR_DELIVERY -> order.setOutForDeliveryTime(now);
            case DELIVERED -> {
                order.setDeliveredTime(now);
                order.setPaymentStatus(Order.PaymentStatus.VERIFIED);
                // Deduct stock only when transitioning into DELIVERED
                if (previousStatus != Order.OrderStatus.DELIVERED) {
                    updateProductStock(order.getOrderItems());
                }
            }
            case CANCELLED -> {
                // Restore stock if this order had previously been delivered
                if (previousStatus == Order.OrderStatus.DELIVERED) {
                    restoreProductStock(order.getOrderItems());
                }
            }
        }
        // If moving away from DELIVERED to a non-cancel status, restore stock as well
        if (previousStatus == Order.OrderStatus.DELIVERED
                && newStatus != Order.OrderStatus.DELIVERED
                && newStatus != Order.OrderStatus.CANCELLED) {
            restoreProductStock(order.getOrderItems());
        }
        
        return orderRepository.save(order);
    }
    
    private void restoreProductStock(List<OrderItem> items) {
        items.forEach(item -> {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        });
    }
    
    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        return orderRepository.save(order);
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // Returns
    public ReturnRequest createReturnRequest(User user, Long orderId, String reason,
                                             Long productId, String phone, String email,
                                             String reasonCategory, java.util.List<String> photoUrls) {
        Order order = getOrderById(orderId);
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized to request return for this order");
        }
        if (order.getStatus() != Order.OrderStatus.DELIVERED || order.getDeliveredTime() == null) {
            throw new RuntimeException("Return allowed only after delivery");
        }
        // 3-day window
        if (order.getDeliveredTime().isBefore(LocalDateTime.now().minusDays(3))) {
            throw new RuntimeException("Return window (3 days) has expired");
        }
        // Only one request per order
        returnRequestRepository.findByOrder(order).ifPresent(r -> {
            throw new RuntimeException("Return already requested for this order");
        });

        ReturnRequest req = new ReturnRequest();
        req.setOrder(order);
        req.setUser(user);
        req.setReason(reason);
        req.setStatus(ReturnRequest.Status.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req.setProductId(productId);
        req.setContactPhone(phone);
        req.setContactEmail(email);
        req.setReasonCategory(reasonCategory);
        req.setPhotoUrls(photoUrls);
        return returnRequestRepository.save(req);
    }

    public ReturnRequest getReturnByOrder(Order order) {
        return returnRequestRepository.findByOrder(order).orElse(null);
    }

    public ReturnRequest updateReturnStatus(Long returnId, ReturnRequest.Status status) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found"));
        req.setStatus(status);
        if (status == ReturnRequest.Status.COMPLETED && req.getCompletedAt() == null) {
            req.setCompletedAt(LocalDateTime.now());
        }
        return returnRequestRepository.save(req);
    }
    
    // Dashboard statistics
    public Long getTodayOrdersCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return orderRepository.countOrdersSince(startOfDay);
    }
    
    public Double getTodayRevenue() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Double revenue = orderRepository.getTotalRevenueSince(startOfDay);
        return revenue != null ? revenue : 0.0;
    }
    
    public Long getThisMonthOrdersCount() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return orderRepository.countOrdersSince(startOfMonth);
    }
    
    public Double getThisMonthRevenue() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Double revenue = orderRepository.getTotalRevenueSince(startOfMonth);
        return revenue != null ? revenue : 0.0;
    }

    // Additional analytics
    public Double getTotalRevenueAllTime() {
        Double rev = orderRepository.getTotalRevenueAllTime();
        return rev != null ? rev : 0.0;
    }

    public Long getItemsSoldLastNDays(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);
        Long val = orderRepository.getTotalItemsSoldSince(start);
        return val != null ? val : 0L;
    }

    public List<java.util.Map<String,Object>> getRevenueTrendLastNDays(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days-1).withHour(0).withMinute(0).withSecond(0);
        return orderRepository.getDailyRevenueSince(start);
    }

    public List<java.util.Map<String,Object>> getTopProducts(int limit) {
        List<Object[]> rows = orderRepository.getTopProducts();
        return rows.stream().limit(limit).map(r -> {
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("productId", r[0]);
            m.put("title", r[1]);
            m.put("quantity", r[2]);
            return m;
        }).toList();
    }

    public Long getTotalOrders() {
        return orderRepository.count();
    }
}
