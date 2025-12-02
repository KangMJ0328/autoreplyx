package com.autoreplyx.service;

import com.autoreplyx.entity.AutoRule;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.AutoRuleRepository;
import com.autoreplyx.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final AutoRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 메시지에 매칭되는 규칙 찾기
     */
    public Optional<AutoRule> findMatchingRule(Long userId, String message, String channel) {
        List<AutoRule> rules = ruleRepository.findActiveRulesByUserAndChannel(userId, channel);

        for (AutoRule rule : rules) {
            // 시간대 체크
            if (!rule.isWithinActiveHours()) {
                log.debug("Rule {} is not within active hours", rule.getId());
                continue;
            }

            // 채널 체크
            if (!rule.supportsChannel(channel)) {
                log.debug("Rule {} does not support channel {}", rule.getId(), channel);
                continue;
            }

            // 매칭 체크
            if (rule.matches(message)) {
                log.info("Message matched rule: {} ({})", rule.getName(), rule.getId());
                return Optional.of(rule);
            }
        }

        log.debug("No matching rule found for message: {}", message);
        return Optional.empty();
    }

    /**
     * 쿨다운 체크
     */
    public boolean isInCooldown(Long ruleId, String senderId) {
        String key = String.format("cooldown:%d:%s", ruleId, senderId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 쿨다운 설정
     */
    public void setCooldown(Long ruleId, String senderId, int minutes) {
        String key = String.format("cooldown:%d:%s", ruleId, senderId);
        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(minutes));
        log.debug("Cooldown set for rule {} sender {} for {} minutes", ruleId, senderId, minutes);
    }

    /**
     * 트리거 카운트 증가
     */
    public void incrementTriggerCount(Long ruleId) {
        ruleRepository.incrementTriggerCount(ruleId);
    }

    /**
     * 응답 텍스트 생성 (링크 포함)
     */
    public String buildResponse(AutoRule rule, User user) {
        StringBuilder response = new StringBuilder(rule.getResponseTemplate());

        if (Boolean.TRUE.equals(rule.getIncludeReservationLink()) && user.getReservationSlug() != null) {
            String reservationUrl = "https://autoreplyx.com/r/" + user.getReservationSlug();
            response.append("\n\n📅 예약하기: ").append(reservationUrl);
        }

        if (Boolean.TRUE.equals(rule.getIncludeEstimateLink()) && user.getReservationSlug() != null) {
            String estimateUrl = "https://autoreplyx.com/e/" + user.getReservationSlug();
            response.append("\n\n📝 견적 요청: ").append(estimateUrl);
        }

        return response.toString();
    }

    /**
     * 메시지에 매칭되는 규칙 찾기 (테스트용 - MatchResult 반환)
     */
    public MatchResult findMatchingRule(String message, Long userId, String channel) {
        List<AutoRule> rules = ruleRepository.findActiveRulesByUserAndChannel(userId, channel.toUpperCase());

        for (AutoRule rule : rules) {
            // 시간대 체크
            if (!rule.isWithinActiveHours()) {
                continue;
            }

            // 채널 체크
            if (!rule.supportsChannel(channel.toUpperCase())) {
                continue;
            }

            // 매칭 체크
            if (rule.matches(message)) {
                User user = userRepository.findById(userId).orElse(null);
                String responseText = user != null ? buildResponse(rule, user) : rule.getResponseTemplate();

                // 쿨다운 체크 (테스트에서는 실제 senderId가 없으므로 test_user 사용)
                boolean wouldTriggerCooldown = isInCooldown(rule.getId(), "test_user");

                return new MatchResult(rule, responseText, wouldTriggerCooldown);
            }
        }

        return null;
    }

    /**
     * 규칙 매칭 결과
     */
    @Getter
    public static class MatchResult {
        private final AutoRule rule;
        private final String responseText;
        private final boolean wouldTriggerCooldown;

        public MatchResult(AutoRule rule, String responseText, boolean wouldTriggerCooldown) {
            this.rule = rule;
            this.responseText = responseText;
            this.wouldTriggerCooldown = wouldTriggerCooldown;
        }
    }
}
