package com.autoreplyx.controller;

import com.autoreplyx.entity.MessageLog;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final MessageLogRepository messageLogRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int per_page,
            @RequestParam(required = false) String channel
    ) {
        User user = getCurrentUser();
        PageRequest pageRequest = PageRequest.of(page - 1, per_page);

        Page<MessageLog> logs;
        if (channel != null && !channel.isEmpty() && !channel.equals("all")) {
            logs = messageLogRepository.findByUserIdAndChannelOrderByCreatedAtDesc(user.getId(), channel, pageRequest);
        } else {
            logs = messageLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageRequest);
        }

        List<Map<String, Object>> data = logs.getContent().stream()
                .map(this::toLogDto)
                .collect(Collectors.toList());

        Map<String, Object> meta = new HashMap<>();
        meta.put("current_page", logs.getNumber() + 1);
        meta.put("last_page", Math.max(1, logs.getTotalPages()));
        meta.put("per_page", logs.getSize());
        meta.put("total", logs.getTotalElements());

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("meta", meta);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam(defaultValue = "csv") String format) {
        User user = getCurrentUser();
        Page<MessageLog> logs = messageLogRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10000)
        );

        if (format.equals("csv")) {
            StringBuilder csv = new StringBuilder();
            csv.append("시간,채널,발신자,수신메시지,응답메시지,타입\n");

            for (MessageLog log : logs.getContent()) {
                csv.append(String.format("%s,%s,%s,\"%s\",\"%s\",%s\n",
                        log.getCreatedAt(),
                        log.getChannel(),
                        log.getSenderId(),
                        log.getReceivedMessage().replace("\"", "\"\""),
                        log.getResponseMessage() != null ? log.getResponseMessage().replace("\"", "\"\"") : "",
                        log.getResponseType()
                ));
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv; charset=utf-8")
                    .header("Content-Disposition", "attachment; filename=message_logs.csv")
                    .body(csv.toString());
        }

        return ResponseEntity.badRequest().body(Map.of("message", "지원하지 않는 형식입니다."));
    }

    private Map<String, Object> toLogDto(MessageLog log) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", log.getId());
        dto.put("channel", log.getChannel());
        dto.put("sender_id", log.getSenderId());
        dto.put("sender_name", log.getSenderName());
        dto.put("received_message", log.getReceivedMessage());
        dto.put("sent_message", log.getResponseMessage());
        dto.put("response_type", log.getResponseType());
        dto.put("created_at", log.getCreatedAt());
        return dto;
    }
}
