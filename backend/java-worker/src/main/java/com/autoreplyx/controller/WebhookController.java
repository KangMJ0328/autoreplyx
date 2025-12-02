package com.autoreplyx.controller;

import com.autoreplyx.entity.Channel;
import com.autoreplyx.entity.MessageLog;
import com.autoreplyx.repository.ChannelRepository;
import com.autoreplyx.repository.MessageLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ChannelRepository channelRepository;
    private final MessageLogRepository messageLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${instagram.verify-token:autoreplyx-verify-token}")
    private String instagramVerifyToken;

    private static final String MESSAGE_QUEUE_KEY = "autoreplyx:message_queue";

    /**
     * Instagram Webhook Verification (GET)
     * Facebook/Instagram sends this to verify the webhook endpoint
     */
    @GetMapping("/instagram")
    public ResponseEntity<?> verifyInstagramWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        log.info("Instagram webhook verification: mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && instagramVerifyToken.equals(token)) {
            log.info("Instagram webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Instagram webhook verification failed");
        return ResponseEntity.status(403).body("Verification failed");
    }

    /**
     * Instagram Webhook Event Handler (POST)
     * Receives incoming messages and comments from Instagram
     */
    @PostMapping("/instagram")
    public ResponseEntity<?> handleInstagramWebhook(@RequestBody String payload) {
        log.info("Received Instagram webhook: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String object = root.path("object").asText();

            if (!"instagram".equals(object)) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            JsonNode entries = root.path("entry");
            for (JsonNode entry : entries) {
                String pageId = entry.path("id").asText();

                // Handle messaging events (DMs)
                JsonNode messaging = entry.path("messaging");
                for (JsonNode event : messaging) {
                    handleMessagingEvent(pageId, event);
                }

                // Handle changes (comments, mentions)
                JsonNode changes = entry.path("changes");
                for (JsonNode change : changes) {
                    handleChangeEvent(pageId, change);
                }
            }

            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (Exception e) {
            log.error("Error processing Instagram webhook", e);
            return ResponseEntity.ok("EVENT_RECEIVED");
        }
    }

    private void handleMessagingEvent(String pageId, JsonNode event) {
        try {
            String senderId = event.path("sender").path("id").asText();
            String recipientId = event.path("recipient").path("id").asText();
            JsonNode message = event.path("message");

            if (message.isMissingNode() || message.has("is_echo")) {
                return; // Skip echo messages
            }

            String messageText = message.path("text").asText();
            String messageId = message.path("mid").asText();

            log.info("Processing DM from {} to {}: {}", senderId, recipientId, messageText);

            // Find the channel by account_id
            Optional<Channel> channelOpt = channelRepository.findByAccountId(recipientId);
            if (channelOpt.isEmpty()) {
                log.warn("No channel found for account: {}", recipientId);
                return;
            }

            Channel channel = channelOpt.get();

            // Create message log entry
            MessageLog messageLog = MessageLog.builder()
                    .userId(channel.getUserId())
                    .channelId(channel.getId())
                    .channel("INSTAGRAM")
                    .senderId(senderId)
                    .receivedMessage(messageText)
                    .responseType("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            messageLogRepository.save(messageLog);

            // Add to processing queue
            Map<String, Object> queueMessage = Map.of(
                    "log_id", messageLog.getId(),
                    "user_id", channel.getUserId(),
                    "channel_id", channel.getId(),
                    "channel_type", "INSTAGRAM",
                    "sender_id", senderId,
                    "message", messageText,
                    "message_id", messageId,
                    "timestamp", System.currentTimeMillis()
            );

            redisTemplate.opsForList().rightPush(
                    MESSAGE_QUEUE_KEY,
                    objectMapper.writeValueAsString(queueMessage)
            );

            log.info("Message queued for processing: logId={}", messageLog.getId());
        } catch (Exception e) {
            log.error("Error handling messaging event", e);
        }
    }

    private void handleChangeEvent(String pageId, JsonNode change) {
        try {
            String field = change.path("field").asText();
            JsonNode value = change.path("value");

            if ("comments".equals(field) || "mentions".equals(field)) {
                String commentId = value.path("id").asText();
                String text = value.path("text").asText();
                String from = value.path("from").path("id").asText();

                log.info("Processing {}: from={}, text={}", field, from, text);

                // Find channel
                Optional<Channel> channelOpt = channelRepository.findByAccountId(pageId);
                if (channelOpt.isEmpty()) {
                    return;
                }

                Channel channel = channelOpt.get();

                // Create message log
                MessageLog messageLog = MessageLog.builder()
                        .userId(channel.getUserId())
                        .channelId(channel.getId())
                        .channel("INSTAGRAM")
                        .senderId(from)
                        .receivedMessage(text)
                        .responseType("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build();

                messageLogRepository.save(messageLog);

                // Queue for processing
                Map<String, Object> queueMessage = Map.of(
                        "log_id", messageLog.getId(),
                        "user_id", channel.getUserId(),
                        "channel_id", channel.getId(),
                        "channel_type", "INSTAGRAM",
                        "event_type", field,
                        "sender_id", from,
                        "message", text,
                        "comment_id", commentId,
                        "timestamp", System.currentTimeMillis()
                );

                redisTemplate.opsForList().rightPush(
                        MESSAGE_QUEUE_KEY,
                        objectMapper.writeValueAsString(queueMessage)
                );
            }
        } catch (Exception e) {
            log.error("Error handling change event", e);
        }
    }

    /**
     * Test endpoint to simulate incoming messages
     */
    @PostMapping("/instagram/test")
    public ResponseEntity<?> testInstagramWebhook(@RequestBody TestMessageRequest request) {
        log.info("Test webhook received: {}", request);

        try {
            // Find any active Instagram channel for testing
            var channels = channelRepository.findAll();
            Channel channel = channels.stream()
                    .filter(c -> "INSTAGRAM".equals(c.getChannelType()) && c.getIsActive())
                    .findFirst()
                    .orElse(null);

            if (channel == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No active Instagram channel found"));
            }

            // Create message log
            MessageLog messageLog = MessageLog.builder()
                    .userId(channel.getUserId())
                    .channelId(channel.getId())
                    .channel("INSTAGRAM")
                    .senderId(request.getSenderId() != null ? request.getSenderId() : "test_user")
                    .receivedMessage(request.getMessage())
                    .responseType("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            messageLogRepository.save(messageLog);

            // Queue for processing
            Map<String, Object> queueMessage = Map.of(
                    "log_id", messageLog.getId(),
                    "user_id", channel.getUserId(),
                    "channel_id", channel.getId(),
                    "channel_type", "INSTAGRAM",
                    "sender_id", request.getSenderId() != null ? request.getSenderId() : "test_user",
                    "message", request.getMessage(),
                    "is_test", true,
                    "timestamp", System.currentTimeMillis()
            );

            redisTemplate.opsForList().rightPush(
                    MESSAGE_QUEUE_KEY,
                    objectMapper.writeValueAsString(queueMessage)
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Test message queued",
                    "log_id", messageLog.getId()
            ));
        } catch (Exception e) {
            log.error("Error processing test message", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @lombok.Data
    public static class TestMessageRequest {
        private String message;
        private String senderId;
    }
}
