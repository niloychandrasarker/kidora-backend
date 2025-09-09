package com.kidora.repository;

import com.kidora.entity.Order;
import com.kidora.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrder(Order order);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT rr FROM ReturnRequest rr JOIN FETCH rr.order o JOIN FETCH rr.user u ORDER BY rr.createdAt DESC")
    List<ReturnRequest> findAllWithOrderAndUser();
}
