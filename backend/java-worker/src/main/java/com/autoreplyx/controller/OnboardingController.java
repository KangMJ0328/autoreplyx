package com.autoreplyx.controller;

import com.autoreplyx.entity.BusinessProfile;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.BusinessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final BusinessProfileRepository businessProfileRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    // 온보딩 상태 조회
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        User user = getCurrentUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (profile == null) {
            response.put("onboarding_completed", false);
            response.put("current_step", 1);
        } else {
            response.put("onboarding_completed", profile.getOnboardingCompleted());
            response.put("current_step", profile.getOnboardingStep());
        }

        return ResponseEntity.ok(response);
    }

    // 전체 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        User user = getCurrentUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElse(BusinessProfile.builder()
                        .userId(user.getId())
                        .onboardingCompleted(false)
                        .onboardingStep(1)
                        .build());

        return ResponseEntity.ok(toDto(profile));
    }

    // Step 1: 계정 연결 정보 저장
    @PostMapping("/step/1")
    public ResponseEntity<?> saveStep1(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setInstagramUrl((String) request.get("instagram_url"));
        profile.setFacebookConnected((Boolean) request.get("facebook_connected"));
        profile.setNaverTalktalkId((String) request.get("naver_talktalk_id"));
        profile.setKakaoChannelId((String) request.get("kakao_channel_id"));
        profile.setOnboardingStep(Math.max(profile.getOnboardingStep(), 2));

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "next_step", 2));
    }

    // Step 2: 상품/서비스 정보 저장
    @PostMapping("/step/2")
    public ResponseEntity<?> saveStep2(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setProductsServices((String) request.get("products_services"));
        profile.setPriceMenu((String) request.get("price_menu"));
        profile.setBusinessHours((String) request.get("business_hours"));
        profile.setLocation((String) request.get("location"));
        profile.setFaqList((String) request.get("faq_list"));
        profile.setOnboardingStep(Math.max(profile.getOnboardingStep(), 3));

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "next_step", 3));
    }

    // Step 3: 예약/상담 방식 저장
    @PostMapping("/step/3")
    public ResponseEntity<?> saveStep3(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setReservationLink((String) request.get("reservation_link"));
        profile.setReservationMethod((String) request.get("reservation_method"));
        profile.setRequiredInfo((String) request.get("required_info"));
        profile.setOnboardingStep(Math.max(profile.getOnboardingStep(), 4));

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "next_step", 4));
    }

    // Step 4: 비즈니스 규칙 저장
    @PostMapping("/step/4")
    public ResponseEntity<?> saveStep4(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setImmediateResponseKeywords((String) request.get("immediate_response_keywords"));
        profile.setBannedTopics((String) request.get("banned_topics"));
        profile.setNightAutoResponse((Boolean) request.get("night_auto_response"));
        profile.setNightResponseMessage((String) request.get("night_response_message"));
        profile.setOnboardingStep(Math.max(profile.getOnboardingStep(), 5));

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "next_step", 5));
    }

    // Step 5: 말투/톤 설정 저장
    @PostMapping("/step/5")
    public ResponseEntity<?> saveStep5(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setToneStyle((String) request.get("tone_style"));
        profile.setToneExample((String) request.get("tone_example"));
        profile.setOnboardingStep(Math.max(profile.getOnboardingStep(), 6));

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다.", "next_step", 6));
    }

    // Step 6: 관리자 정보 저장 및 온보딩 완료
    @PostMapping("/step/6")
    public ResponseEntity<?> saveStep6(@RequestBody Map<String, Object> request) {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());

        profile.setAdminEmail((String) request.get("admin_email"));
        profile.setAdminKakao((String) request.get("admin_kakao"));
        profile.setAlertKeywords((String) request.get("alert_keywords"));
        profile.setOnboardingCompleted(true);
        profile.setOnboardingStep(6);

        businessProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "온보딩이 완료되었습니다!", "onboarding_completed", true));
    }

    // 온보딩 스킵
    @PostMapping("/skip")
    public ResponseEntity<?> skipOnboarding() {
        User user = getCurrentUser();
        BusinessProfile profile = getOrCreateProfile(user.getId());
        profile.setOnboardingCompleted(true);
        businessProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of("message", "온보딩을 건너뛰었습니다.", "onboarding_completed", true));
    }

    private BusinessProfile getOrCreateProfile(Long userId) {
        return businessProfileRepository.findByUserId(userId)
                .orElse(BusinessProfile.builder()
                        .userId(userId)
                        .onboardingCompleted(false)
                        .onboardingStep(1)
                        .build());
    }

    private Map<String, Object> toDto(BusinessProfile profile) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", profile.getId());
        dto.put("user_id", profile.getUserId());

        // Step 1
        dto.put("instagram_url", profile.getInstagramUrl());
        dto.put("facebook_connected", profile.getFacebookConnected());
        dto.put("naver_talktalk_id", profile.getNaverTalktalkId());
        dto.put("kakao_channel_id", profile.getKakaoChannelId());

        // Step 2
        dto.put("products_services", profile.getProductsServices());
        dto.put("price_menu", profile.getPriceMenu());
        dto.put("business_hours", profile.getBusinessHours());
        dto.put("location", profile.getLocation());
        dto.put("faq_list", profile.getFaqList());

        // Step 3
        dto.put("reservation_link", profile.getReservationLink());
        dto.put("reservation_method", profile.getReservationMethod());
        dto.put("required_info", profile.getRequiredInfo());

        // Step 4
        dto.put("immediate_response_keywords", profile.getImmediateResponseKeywords());
        dto.put("banned_topics", profile.getBannedTopics());
        dto.put("night_auto_response", profile.getNightAutoResponse());
        dto.put("night_response_message", profile.getNightResponseMessage());

        // Step 5
        dto.put("tone_style", profile.getToneStyle());
        dto.put("tone_example", profile.getToneExample());

        // Step 6
        dto.put("admin_email", profile.getAdminEmail());
        dto.put("admin_kakao", profile.getAdminKakao());
        dto.put("alert_keywords", profile.getAlertKeywords());

        // Status
        dto.put("onboarding_completed", profile.getOnboardingCompleted());
        dto.put("onboarding_step", profile.getOnboardingStep());

        return dto;
    }
}
