package com.autoreplyx.service;

import com.autoreplyx.entity.*;
import com.autoreplyx.model.IncomingMessage;
import com.autoreplyx.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessor {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MessageLogRepository messageLogRepository;
    private final RuleEngineService ruleEngineService;
    private final AIService aiService;
    private final InstagramService instagramService;

    @Value("${worker.message-queue}")
    private String messageQueue;

    @Value("${worker.poll-timeout}")
    private int pollTimeout;

    @Value("${worker.thread-pool-size}")
    private int threadPoolSize;

    @Value("${worker.max-retries}")
    private int maxRetries;

    private ExecutorService executorService;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("MessageProcessor initialized with {} threads", threadPoolSize);

        // 워커 스레드 시작
        for (int i = 0; i < threadPoolSize; i++) {
            executorService.submit(this::processLoop);
        }
    }

    /**
     * 메시지 처리 루프
     */
    private void processLoop() {
        while (running) {
            try {
                // 블로킹 팝으로 메시지 대기
                String payload = redisTemplate.opsForList()
                        .rightPop(messageQueue, Duration.ofSeconds(pollTimeout));

                if (payload != null) {
                    processMessage(payload);
                }

            } catch (Exception e) {
                log.error("Error in message processing loop", e);
                try {
                    Thread.sleep(1000); // 에러 시 잠시 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 개별 메시지 처리
     */
    private void processMessage(String payload) {
        long startTime = System.currentTimeMillis();

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.get("data");

            if (data == null) {
                log.warn("Invalid message payload: missing data");
                return;
            }

            IncomingMessage message = objectMapper.treeToValue(data, IncomingMessage.class);

            log.info("Processing message: type={}, channel={}, userId={}",
                    message.getType(), message.getChannel(), message.getUserId());

            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(message.getUserId());
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", message.getUserId());
                return;
            }
            User user = userOpt.get();

            // 채널 조회
            Optional<Channel> channelOpt = channelRepository.findByIdAndIsActiveTrue(message.getChannelId());
            if (channelOpt.isEmpty()) {
                log.warn("Channel not found or inactive: {}", message.getChannelId());
                return;
            }
            Channel channel = channelOpt.get();

            // 메시지 타입별 처리
            String responseText = null;
            String responseType = "none";
            Long matchedRuleId = null;
            int aiTokensUsed = 0;

            if ("message".equals(message.getType())) {
                // 규칙 매칭 시도
                Optional<AutoRule> ruleOpt = ruleEngineService.findMatchingRule(
                        user.getId(),
                        message.getText(),
                        message.getChannel()
                );

                if (ruleOpt.isPresent()) {
                    AutoRule rule = ruleOpt.get();

                    // 쿨다운 체크
                    if (!ruleEngineService.isInCooldown(rule.getId(), message.getSenderId())) {
                        responseText = ruleEngineService.buildResponse(rule, user);
                        responseType = "rule";
                        matchedRuleId = rule.getId();

                        // 쿨다운 설정 (초를 분으로 변환)
                        ruleEngineService.setCooldown(
                                rule.getId(),
                                message.getSenderId(),
                                rule.getCooldownSeconds() != null ? rule.getCooldownSeconds() / 60 : 1
                        );

                        // 트리거 카운트 증가
                        ruleEngineService.incrementTriggerCount(rule.getId());
                    } else {
                        log.debug("Rule {} is in cooldown for sender {}", rule.getId(), message.getSenderId());
                    }
                } else if (Boolean.TRUE.equals(user.getAiEnabled())) {
                    // AI 응답 생성
                    AIService.AIResponse aiResponse = aiService.generateResponse(message.getText(), user);
                    responseText = aiResponse.text();
                    responseType = "ai";
                    aiTokensUsed = aiResponse.tokensUsed();
                }
            }

            // 응답 전송
            if (responseText != null && !responseText.isEmpty()) {
                boolean sent = sendResponse(channel, message, responseText);

                if (!sent) {
                    responseType = "none";
                    log.error("Failed to send response");
                }
            }

            // 처리 시간 계산
            int processingTimeMs = (int) (System.currentTimeMillis() - startTime);

            // 로그 저장
            MessageLog logEntry = MessageLog.builder()
                    .userId(user.getId())
                    .channel(message.getChannel())
                    .senderId(message.getSenderId())
                    .senderName(message.getSenderName())
                    .receivedMessage(message.getText())
                    .responseMessage(responseText)
                    .responseType(responseType)
                    .matchedRuleId(matchedRuleId)
                    .aiTokensUsed(aiTokensUsed)
                    .processingTimeMs(processingTimeMs)
                    .build();

            messageLogRepository.save(logEntry);

            log.info("Message processed: type={}, responseType={}, time={}ms",
                    message.getType(), responseType, processingTimeMs);

        } catch (Exception e) {
            log.error("Failed to process message", e);

            // 재시도 큐로 이동
            try {
                JsonNode root = objectMapper.readTree(payload);
                int retryCount = root.path("data").path("retryCount").asInt(0);

                if (retryCount < maxRetries) {
                    // 재시도 큐로 이동
                    redisTemplate.opsForList().leftPush(messageQueue + ".retry", payload);
                } else {
                    // 실패 큐로 이동
                    redisTemplate.opsForList().leftPush(messageQueue + ".failed", payload);
                }
            } catch (Exception ex) {
                log.error("Failed to move message to retry queue", ex);
            }
        }
    }

    /**
     * 응답 전송
     */
    private boolean sendResponse(Channel channel, IncomingMessage message, String responseText) {
        if ("instagram".equals(channel.getChannelType())) {
            return instagramService.sendMessage(
                    channel.getAccessToken(),
                    message.getSenderId(),
                    responseText
            );
        }

        // 다른 채널 지원 추가
        log.warn("Unsupported channel type: {}", channel.getChannelType());
        return false;
    }

    /**
     * 재시도 큐 처리 (5분마다)
     */
    @Scheduled(fixedRate = 300000)
    public void processRetryQueue() {
        String retryQueue = messageQueue + ".retry";
        Long queueLength = redisTemplate.opsForList().size(retryQueue);

        if (queueLength == null || queueLength == 0) {
            return;
        }

        log.info("Processing retry queue: {} messages", queueLength);

        for (int i = 0; i < queueLength; i++) {
            String payload = redisTemplate.opsForList().rightPop(retryQueue);
            if (payload != null) {
                // 메인 큐로 다시 추가 (재시도 카운트 증가됨)
                redisTemplate.opsForList().leftPush(messageQueue, payload);
            }
        }
    }

    /**
     * 종료 처리
     */
    public void shutdown() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
