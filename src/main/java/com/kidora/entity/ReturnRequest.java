package com.kidora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Additional details
    @Column(name = "product_id")
    private Long productId; // product being returned (optional if whole order)

    private String reasonCategory; // e.g., DAMAGED, WRONG_SIZE, NOT_AS_DESCRIBED

    private String contactPhone;
    private String contactEmail;

    @ElementCollection
    @CollectionTable(name = "return_request_photos", joinColumns = @JoinColumn(name = "return_request_id"))
    @Column(name = "photo_url")
    private java.util.List<String> photoUrls;

    // Timestamp when return marked successful (COMPLETED)
    private LocalDateTime completedAt;

    public enum Status { PENDING, APPROVED, REJECTED, COMPLETED }
}
