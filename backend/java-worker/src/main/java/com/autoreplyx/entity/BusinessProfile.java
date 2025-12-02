package com.autoreplyx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // ============ 1. 계정 연결 정보 ============
    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "facebook_connected")
    private Boolean facebookConnected;

    @Column(name = "naver_talktalk_id")
    private String naverTalktalkId;

    @Column(name = "kakao_channel_id")
    private String kakaoChannelId;

    // ============ 2. 상품/서비스 정보 ============
    @Column(name = "products_services", columnDefinition = "TEXT")
    private String productsServices;  // JSON 형태로 저장

    @Column(name = "price_menu", columnDefinition = "TEXT")
    private String priceMenu;

    @Column(name = "business_hours")
    private String businessHours;

    @Column(name = "location")
    private String location;

    @Column(name = "faq_list", columnDefinition = "TEXT")
    private String faqList;  // JSON 배열 형태로 저장

    // ============ 3. 예약/상담 방식 ============
    @Column(name = "reservation_link")
    private String reservationLink;

    @Column(name = "reservation_method", columnDefinition = "TEXT")
    private String reservationMethod;

    @Column(name = "required_info", columnDefinition = "TEXT")
    private String requiredInfo;  // 상담 시 필요한 필수 정보

    // ============ 4. 비즈니스 규칙 ============
    @Column(name = "immediate_response_keywords", columnDefinition = "TEXT")
    private String immediateResponseKeywords;

    @Column(name = "banned_topics", columnDefinition = "TEXT")
    private String bannedTopics;  // 응답하면 안 되는 정보

    @Column(name = "night_auto_response")
    private Boolean nightAutoResponse;

    @Column(name = "night_response_message", columnDefinition = "TEXT")
    private String nightResponseMessage;

    // ============ 5. 말투/톤 설정 ============
    @Column(name = "tone_style")
    private String toneStyle;  // formal, friendly, casual

    @Column(name = "tone_example", columnDefinition = "TEXT")
    private String toneExample;

    // ============ 6. 관리자 정보 ============
    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "admin_kakao")
    private String adminKakao;

    @Column(name = "alert_keywords", columnDefinition = "TEXT")
    private String alertKeywords;  // 중요한 메시지 알림 기준

    // ============ 온보딩 상태 ============
    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted;

    @Column(name = "onboarding_step")
    private Integer onboardingStep;  // 현재 진행 단계 (1~6)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (onboardingCompleted == null) {
            onboardingCompleted = false;
        }
        if (onboardingStep == null) {
            onboardingStep = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
