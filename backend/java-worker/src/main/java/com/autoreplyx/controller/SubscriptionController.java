package com.autoreplyx.controller;

import com.autoreplyx.entity.Subscription;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.MessageLogRepository;
import com.autoreplyx.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final MessageLogRepository messageLogRepository;

    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal User user) {
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultSubscription(user.getId()));

        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgrade(
            @AuthenticationPrincipal User user,
            @RequestBody UpgradeRequest request) {

        // 실제 결제 연동 시 Stripe Checkout Session 생성
        // 여기서는 테스트용 checkout URL 반환
        String checkoutUrl = "https://checkout.stripe.com/test?plan=" + request.getPlan();

        return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl));
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal User user) {
        return subscriptionRepository.findByUserId(user.getId())
                .map(subscription -> {
                    subscription.setStatus("cancelled");
                    subscription.setCancelledAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                    return ResponseEntity.ok(Map.of("message", "구독이 취소되었습니다."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/resume")
    public ResponseEntity<?> resume(@AuthenticationPrincipal User user) {
        return subscriptionRepository.findByUserId(user.getId())
                .map(subscription -> {
                    if ("cancelled".equals(subscription.getStatus())) {
                        subscription.setStatus("active");
                        subscription.setCancelledAt(null);
                        Subscription saved = subscriptionRepository.save(subscription);
                        return ResponseEntity.ok(saved);
                    }
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "취소된 구독만 재개할 수 있습니다."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usage")
    public ResponseEntity<?> getUsage(@AuthenticationPrincipal User user) {
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultSubscription(user.getId()));

        // 오늘 메시지 수 계산
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayMessages = messageLogRepository.countByUserIdAndCreatedAtBetween(
                user.getId(), todayStart, todayEnd);

        // AI 토큰 사용량 (이번 달)
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long aiTokensUsed = messageLogRepository.sumAiTokensUsedByUserIdAndCreatedAtBetween(
                user.getId(), monthStart, LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
                "today_messages", todayMessages,
                "daily_limit", subscription.getDailyMessageLimit(),
                "ai_tokens_used", aiTokensUsed != null ? aiTokensUsed : 0,
                "plan", subscription.getPlan()
        ));
    }

    private Subscription createDefaultSubscription(Long userId) {
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .plan("free")
                .status("active")
                .build();
        subscription.applyPlanDefaults();
        return subscriptionRepository.save(subscription);
    }

    @lombok.Data
    public static class UpgradeRequest {
        private String plan;
    }
}
