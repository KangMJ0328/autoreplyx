package com.autoreplyx.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "autoreplyx-worker");
        response.put("timestamp", LocalDateTime.now().toString());

        // Redis 연결 체크
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            response.put("redis", "connected");
        } catch (Exception e) {
            response.put("redis", "disconnected");
            response.put("redis_error", e.getMessage());
        }

        // 큐 상태
        try {
            Long messageQueueSize = redisTemplate.opsForList().size("message.queue");
            Long retryQueueSize = redisTemplate.opsForList().size("message.queue.retry");
            Long failedQueueSize = redisTemplate.opsForList().size("message.queue.failed");

            Map<String, Long> queues = new HashMap<>();
            queues.put("message.queue", messageQueueSize != null ? messageQueueSize : 0);
            queues.put("message.queue.retry", retryQueueSize != null ? retryQueueSize : 0);
            queues.put("message.queue.failed", failedQueueSize != null ? failedQueueSize : 0);

            response.put("queues", queues);
        } catch (Exception e) {
            response.put("queues_error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
