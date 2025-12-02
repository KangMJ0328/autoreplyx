package com.autoreplyx.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "received_message", nullable = false, columnDefinition = "TEXT")
    private String receivedMessage;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "response_type", nullable = false)
    private String responseType; // rule, ai, manual, none

    @Column(name = "matched_rule_id")
    private Long matchedRuleId;

    @Column(name = "ai_tokens_used")
    private Integer aiTokensUsed = 0;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
