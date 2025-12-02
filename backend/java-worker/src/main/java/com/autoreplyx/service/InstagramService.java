package com.autoreplyx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class InstagramService {

    private final RestTemplate restTemplate;

    @Value("${instagram.api-url}")
    private String apiUrl;

    public InstagramService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Instagram DM 전송
     */
    public boolean sendMessage(String accessToken, String recipientId, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("id", recipientId);

            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("text", message);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipient", recipient);
            requestBody.put("message", messageBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = apiUrl + "/me/messages";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Instagram message sent successfully to {}", recipientId);
                return true;
            }

            log.error("Instagram send message failed: {}", response.getBody());
            return false;

        } catch (Exception e) {
            log.error("Failed to send Instagram message to {}", recipientId, e);
            return false;
        }
    }

    /**
     * Instagram 댓글 답글 작성
     */
    public boolean replyToComment(String accessToken, String commentId, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = apiUrl + "/" + commentId + "/replies";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Instagram comment reply sent successfully to {}", commentId);
                return true;
            }

            log.error("Instagram comment reply failed: {}", response.getBody());
            return false;

        } catch (Exception e) {
            log.error("Failed to reply to Instagram comment {}", commentId, e);
            return false;
        }
    }
}
