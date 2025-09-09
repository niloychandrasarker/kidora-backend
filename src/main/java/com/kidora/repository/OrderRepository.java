package com.kidora.repository;

import com.kidora.entity.Order;
import com.kidora.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long countOrdersSince(LocalDateTime startDate);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :startDate")
    Double getTotalRevenueSince(LocalDateTime startDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double getTotalRevenueAllTime();

    @Query("SELECT COALESCE(SUM(oi.quantity),0) FROM Order o JOIN o.orderItems oi WHERE o.createdAt >= :startDate")
    Long getTotalItemsSoldSince(LocalDateTime startDate);

    @Query("SELECT NEW map(FUNCTION('date', o.createdAt) as day, SUM(o.totalAmount) as amount) FROM Order o WHERE o.createdAt >= :startDate GROUP BY FUNCTION('date', o.createdAt) ORDER BY day ASC")
    List<java.util.Map<String,Object>> getDailyRevenueSince(LocalDateTime startDate);

    @Query("SELECT oi.product.id, oi.productTitle, SUM(oi.quantity) as qty FROM Order o JOIN o.orderItems oi GROUP BY oi.product.id, oi.productTitle ORDER BY qty DESC")
    List<Object[]> getTopProducts();
    
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<Order> findByStatusOrderByCreatedAtAsc(Order.OrderStatus status);
}
