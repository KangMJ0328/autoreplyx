package com.autoreplyx.service;

import com.autoreplyx.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class AIService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.max-tokens:200}")
    private int maxTokens;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public AIService(RedisTemplate<String, String> redisTemplate) {
        this.restTemplate = new RestTemplate();
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * AI 응답 생성
     */
    public AIResponse generateResponse(String message, User user) {
        // 캐시 체크
        String cacheKey = "ai_response:" + hashMessage(message, user.getId());
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.debug("AI response cache hit for user {}", user.getId());
            return new AIResponse(cached, 0, true);
        }

        // API가 설정되지 않은 경우
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured");
            return new AIResponse(getFallbackResponse(), 0, false);
        }

        try {
            String systemPrompt = buildSystemPrompt(user);
            String fullPrompt = systemPrompt + "\n\n고객 메시지: " + message;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gemini API 요청 형식
            Map<String, Object> requestBody = new HashMap<>();

            // contents 배열
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();

            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", fullPrompt));
            content.put("parts", parts);
            contents.add(content);

            requestBody.put("contents", contents);

            // generationConfig
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("temperature", 0.7);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = String.format(GEMINI_API_URL, model, apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                // Gemini 응답 파싱
                String responseText = root.path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                // 토큰 사용량 (Gemini는 usageMetadata에서 제공)
                int tokensUsed = root.path("usageMetadata").path("totalTokenCount").asInt(0);

                // 금지어 필터링
                responseText = filterBannedWords(responseText, user.getBannedWords());

                // 캐시 저장 (24시간)
                redisTemplate.opsForValue().set(cacheKey, responseText, Duration.ofHours(24));

                log.info("Gemini response generated for user {}, tokens: {}", user.getId(), tokensUsed);
                return new AIResponse(responseText, tokensUsed, false);
            }

            log.error("Gemini API error: {}", response.getStatusCode());
            return new AIResponse(getFallbackResponse(), 0, false);

        } catch (Exception e) {
            log.error("AI response generation failed", e);
            return new AIResponse(getFallbackResponse(), 0, false);
        }
    }

    /**
     * 시스템 프롬프트 생성
     */
    private String buildSystemPrompt(User user) {
        String toneGuide = getToneGuide(user.getAiTone());

        return String.format("""
                당신은 %s의 고객 응대 AI 어시스턴트입니다.

                [비즈니스 정보]
                - 영업시간: %s
                - 주소: %s
                - 소개: %s

                [응답 규칙]
                1. %s
                2. 150자 이내로 간결하게 응답하세요.
                3. 확실하지 않은 정보는 "확인 후 안내드리겠습니다"라고 응답하세요.
                4. 고객의 질문에 직접적으로 답변하세요.
                5. 이모지를 적절히 사용해 친근한 느낌을 주세요.
                """,
                user.getBrandName(),
                user.getBusinessHours() != null ? user.getBusinessHours() : "미설정",
                user.getAddress() != null ? user.getAddress() : "미설정",
                user.getDescription() != null ? user.getDescription() : "",
                toneGuide
        );
    }

    /**
     * 응답 톤 가이드
     */
    private String getToneGuide(String tone) {
        if (tone == null) tone = "friendly";

        return switch (tone) {
            case "professional" -> "전문적이고 신뢰감 있는 톤으로 응답하세요.";
            case "formal" -> "격식을 차린 공손한 톤으로 응답하세요.";
            case "casual" -> "편안하고 캐주얼한 톤으로 응답하세요.";
            default -> "친근하고 따뜻한 톤으로 응답하세요.";
        };
    }

    /**
     * 금지어 필터링
     */
    private String filterBannedWords(String text, String bannedWordsJson) {
        if (bannedWordsJson == null || bannedWordsJson.isEmpty()) {
            return text;
        }

        try {
            List<String> bannedWords = objectMapper.readValue(bannedWordsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            for (String word : bannedWords) {
                text = text.replaceAll("(?i)" + word, "***");
            }
        } catch (Exception e) {
            log.warn("Failed to parse banned words", e);
        }

        return text;
    }

    /**
     * 폴백 응답
     */
    private String getFallbackResponse() {
        String[] responses = {
                "안녕하세요! 문의 주셔서 감사합니다. 잠시 후 담당자가 답변드리겠습니다.",
                "안녕하세요! 문의 내용 확인 후 빠르게 답변드리겠습니다.",
                "감사합니다! 조금만 기다려주시면 자세한 안내 도와드리겠습니다."
        };
        return responses[new Random().nextInt(responses.length)];
    }

    /**
     * 메시지 해시 생성
     */
    private String hashMessage(String message, Long userId) {
        return Integer.toHexString((message + userId).hashCode());
    }

    /**
     * AI 응답 결과
     */
    public record AIResponse(String text, int tokensUsed, boolean cached) {}
}
