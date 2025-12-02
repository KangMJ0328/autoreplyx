package com.autoreplyx.controller;

import com.autoreplyx.entity.AutoRule;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.AutoRuleRepository;
import com.autoreplyx.service.AIService;
import com.autoreplyx.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final RuleEngineService ruleEngineService;
    private final AutoRuleRepository autoRuleRepository;

    /**
     * 메시지 테스트 - 규칙 매칭 또는 AI 응답 미리보기
     */
    @PostMapping("/test-message")
    public Map<String, Object> testMessage(
            @AuthenticationPrincipal User user,
            @RequestBody TestMessageRequest request) {

        Map<String, Object> result = new HashMap<>();

        // 1. 규칙 매칭 시도
        String channel = request.getChannel() != null ? request.getChannel() : "instagram";
        RuleEngineService.MatchResult matchResult = ruleEngineService.findMatchingRule(
                request.getMessage(), user.getId(), channel);

        if (matchResult != null && matchResult.getRule() != null) {
            AutoRule rule = matchResult.getRule();
            result.put("matched_rule", Map.of(
                    "id", rule.getId(),
                    "name", rule.getName(),
                    "match_type", rule.getMatchType(),
                    "keyword", rule.getKeywords()
            ));
            result.put("response_type", "rule");
            result.put("response_text", matchResult.getResponseText());
            result.put("ai_tokens_used", 0);
            result.put("cached", false);
            result.put("would_trigger_cooldown", matchResult.isWouldTriggerCooldown());
        } else {
            // 2. AI 응답 생성
            AIService.AIResponse aiResponse = aiService.generateResponse(request.getMessage(), user);
            result.put("matched_rule", null);
            result.put("response_type", "ai");
            result.put("response_text", aiResponse.text());
            result.put("ai_tokens_used", aiResponse.tokensUsed());
            result.put("cached", aiResponse.cached());
            result.put("would_trigger_cooldown", false);
        }

        return result;
    }

    /**
     * AI로 FAQ 자동 생성
     */
    @PostMapping("/generate-faq")
    public Map<String, Object> generateFaq(
            @AuthenticationPrincipal User user,
            @RequestBody GenerateFaqRequest request) {

        int count = request.getCount() != null ? request.getCount() : 5;
        count = Math.min(count, 10); // 최대 10개

        List<Map<String, String>> faqs = new ArrayList<>();
        int totalTokens = 0;

        // 업종에 따른 FAQ 템플릿 생성
        List<String> questions = getIndustryFaqQuestions(user.getIndustry(), count);

        for (String question : questions) {
            AIService.AIResponse response = aiService.generateResponse(question, user);
            faqs.add(Map.of(
                    "keyword", question,
                    "response", response.text()
            ));
            totalTokens += response.tokensUsed();
        }

        return Map.of(
                "faqs", faqs,
                "tokens_used", totalTokens
        );
    }

    /**
     * 단일 AI 응답 생성
     */
    @PostMapping("/generate-response")
    public Map<String, Object> generateResponse(
            @AuthenticationPrincipal User user,
            @RequestBody GenerateResponseRequest request) {

        AIService.AIResponse response = aiService.generateResponse(request.getMessage(), user);

        return Map.of(
                "response", response.text(),
                "tokens_used", response.tokensUsed(),
                "cached", response.cached()
        );
    }

    /**
     * 업종별 FAQ 질문 템플릿
     */
    private List<String> getIndustryFaqQuestions(String industry, int count) {
        List<String> questions = switch (industry != null ? industry : "other") {
            case "cafe" -> List.of(
                    "영업시간이 어떻게 되나요?",
                    "주차 가능한가요?",
                    "예약 가능한가요?",
                    "와이파이 있나요?",
                    "노트북 작업 가능한가요?",
                    "반려동물 출입 가능한가요?",
                    "케이크 예약 가능한가요?",
                    "단체 예약 가능한가요?",
                    "인기 메뉴가 뭔가요?",
                    "테이크아웃 되나요?"
            );
            case "restaurant" -> List.of(
                    "영업시간이 어떻게 되나요?",
                    "예약 가능한가요?",
                    "주차 가능한가요?",
                    "단체석 있나요?",
                    "포장 되나요?",
                    "배달 되나요?",
                    "룸이 있나요?",
                    "아이 동반 가능한가요?",
                    "인기 메뉴가 뭔가요?",
                    "알레르기 표기가 있나요?"
            );
            case "beauty" -> List.of(
                    "예약 어떻게 하나요?",
                    "영업시간이 어떻게 되나요?",
                    "주차 가능한가요?",
                    "가격표 있나요?",
                    "당일 예약 가능한가요?",
                    "남성 손님도 되나요?",
                    "펌 가격이 어떻게 되나요?",
                    "염색 가격이 어떻게 되나요?",
                    "소요시간이 얼마나 걸리나요?",
                    "시술 전 상담 가능한가요?"
            );
            case "shopping" -> List.of(
                    "배송비가 얼마인가요?",
                    "교환/반품 어떻게 하나요?",
                    "배송 얼마나 걸려요?",
                    "재고 있나요?",
                    "쿠폰 있나요?",
                    "적립금 사용 가능한가요?",
                    "사이즈 교환 가능한가요?",
                    "해외배송 되나요?",
                    "결제 방법이 뭐가 있나요?",
                    "품절 상품 재입고 되나요?"
            );
            case "freelance" -> List.of(
                    "견적 요청 어떻게 하나요?",
                    "작업 기간이 얼마나 걸려요?",
                    "포트폴리오 볼 수 있나요?",
                    "수정은 몇 번까지 되나요?",
                    "결제는 어떻게 하나요?",
                    "계약서 작성하나요?",
                    "급한 작업 가능한가요?",
                    "미팅 가능한가요?",
                    "이전 작업 사례 있나요?",
                    "A/S 기간이 있나요?"
            );
            default -> List.of(
                    "영업시간이 어떻게 되나요?",
                    "위치가 어디인가요?",
                    "예약 어떻게 하나요?",
                    "문의는 어디로 하나요?",
                    "주차 가능한가요?"
            );
        };

        return questions.subList(0, Math.min(count, questions.size()));
    }

    @lombok.Data
    public static class TestMessageRequest {
        private String message;
        private String channel;
    }

    @lombok.Data
    public static class GenerateFaqRequest {
        private Integer count;
    }

    @lombok.Data
    public static class GenerateResponseRequest {
        private String message;
    }
}
