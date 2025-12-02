package com.autoreplyx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private String plan = "free"; // free, pro, agency

    @Column(nullable = false)
    @Builder.Default
    private String status = "active"; // active, trialing, cancelled, expired, past_due

    @Column(name = "price_monthly", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceMonthly = BigDecimal.ZERO;

    @Column(name = "daily_message_limit")
    private Integer dailyMessageLimit;

    @Column(columnDefinition = "JSON")
    private String features;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentPeriodStart == null) {
            currentPeriodStart = LocalDateTime.now();
        }
        if (currentPeriodEnd == null) {
            currentPeriodEnd = LocalDateTime.now().plusMonths(1);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 플랜별 기본 설정 적용
     */
    public void applyPlanDefaults() {
        switch (plan) {
            case "free" -> {
                priceMonthly = BigDecimal.ZERO;
                dailyMessageLimit = 50;
                features = "{\"ai_enabled\": true, \"custom_rules\": 5, \"channels\": 1}";
            }
            case "pro" -> {
                priceMonthly = new BigDecimal("29000");
                dailyMessageLimit = 500;
                features = "{\"ai_enabled\": true, \"custom_rules\": 50, \"channels\": 3, \"analytics\": true}";
            }
            case "agency" -> {
                priceMonthly = new BigDecimal("99000");
                dailyMessageLimit = null; // unlimited
                features = "{\"ai_enabled\": true, \"custom_rules\": -1, \"channels\": -1, \"analytics\": true, \"api_access\": true}";
            }
        }
    }
}
