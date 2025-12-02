package com.autoreplyx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estimate_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String budget;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending"; // pending, quoted, accepted, rejected, completed

    @Column(name = "quoted_amount", precision = 12, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "quote_message", columnDefinition = "TEXT")
    private String quoteMessage;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "source")
    @Builder.Default
    private String source = "web"; // chat, web, manual

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
