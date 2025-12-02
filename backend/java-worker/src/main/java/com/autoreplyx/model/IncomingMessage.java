package com.autoreplyx.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomingMessage {
    private String id;
    private String type;           // message, comment, mention
    private String channel;        // instagram, kakao, naver
    private Long userId;
    private Long channelId;
    private Long eventId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String messageId;
    private String text;
    private Long timestamp;
    private boolean isTest;
    private int retryCount;
    private String retryAt;
    private String createdAt;
}
