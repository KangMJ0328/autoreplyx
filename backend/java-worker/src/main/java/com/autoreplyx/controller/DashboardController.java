package com.autoreplyx.controller;

import com.autoreplyx.entity.MessageLog;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.ChannelRepository;
import com.autoreplyx.repository.MessageLogRepository;
import com.autoreplyx.repository.ReservationRepository;
import com.autoreplyx.repository.AutoRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MessageLogRepository messageLogRepository;
    private final ChannelRepository channelRepository;
    private final ReservationRepository reservationRepository;
    private final AutoRuleRepository autoRuleRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        User user = getCurrentUser();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);

        long totalMessages = messageLogRepository.countByUserIdAndCreatedAtAfter(user.getId(), thirtyDaysAgo);
        long aiResponses = messageLogRepository.countByUserIdAndResponseTypeAndCreatedAtAfter(user.getId(), "AI", thirtyDaysAgo);
        long ruleResponses = messageLogRepository.countByUserIdAndResponseTypeAndCreatedAtAfter(user.getId(), "RULE", thirtyDaysAgo);

        long prevTotalMessages = messageLogRepository.countByUserIdAndCreatedAtBetween(user.getId(), sixtyDaysAgo, thirtyDaysAgo);
        long prevAiResponses = messageLogRepository.countByUserIdAndResponseTypeAndCreatedAtBetween(user.getId(), "AI", sixtyDaysAgo, thirtyDaysAgo);
        long prevRuleResponses = messageLogRepository.countByUserIdAndResponseTypeAndCreatedAtBetween(user.getId(), "RULE", sixtyDaysAgo, thirtyDaysAgo);

        Map<String, Object> response = new HashMap<>();
        response.put("total_messages", totalMessages);
        response.put("ai_responses", aiResponses);
        response.put("rule_responses", ruleResponses);
        response.put("message_change", calculateChange(totalMessages, prevTotalMessages));
        response.put("ai_change", calculateChange(aiResponses, prevAiResponses));
        response.put("rule_change", calculateChange(ruleResponses, prevRuleResponses));

        return ResponseEntity.ok(response);
    }

    private String calculateChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "+0%";
        }
        double change = ((double) (current - previous) / previous) * 100;
        return String.format("%s%.1f%%", change >= 0 ? "+" : "", change);
    }

    // 차트 데이터 (최근 7일 일별 메시지 수)
    @GetMapping("/chart")
    public ResponseEntity<?> chartData(@RequestParam(defaultValue = "7") int days) {
        User user = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Object[]> dailyData = messageLogRepository.countByUserIdGroupByDate(user.getId(), since);

        // 날짜별 데이터를 Map으로 변환
        Map<String, Long> dataMap = new LinkedHashMap<>();
        for (Object[] row : dailyData) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            dataMap.put(date, count);
        }

        // 최근 N일 데이터 생성 (데이터 없는 날도 0으로 채움)
        List<Map<String, Object>> chartData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(formatter);
            Map<String, Object> point = new HashMap<>();
            point.put("date", dateStr);
            point.put("label", date.format(DateTimeFormatter.ofPattern("M/d")));
            point.put("count", dataMap.getOrDefault(dateStr, 0L));
            chartData.add(point);
        }

        return ResponseEntity.ok(chartData);
    }

    // 채널별 통계
    @GetMapping("/channel-stats")
    public ResponseEntity<?> channelStats() {
        User user = getCurrentUser();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Object[]> channelData = messageLogRepository.countByUserIdGroupByChannel(user.getId(), thirtyDaysAgo);

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : channelData) {
            Map<String, Object> item = new HashMap<>();
            item.put("channel", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            stats.add(item);
        }

        return ResponseEntity.ok(stats);
    }

    // 응답 타입별 통계
    @GetMapping("/response-stats")
    public ResponseEntity<?> responseStats() {
        User user = getCurrentUser();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Object[]> responseData = messageLogRepository.countByUserIdGroupByResponseType(user.getId(), thirtyDaysAgo);

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : responseData) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            stats.add(item);
        }

        return ResponseEntity.ok(stats);
    }

    // 최근 활동
    @GetMapping("/recent-activity")
    public ResponseEntity<?> recentActivity(@RequestParam(defaultValue = "10") int limit) {
        User user = getCurrentUser();

        List<MessageLog> logs = messageLogRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> activities = new ArrayList<>();
        for (MessageLog log : logs) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", log.getId());
            activity.put("channel", log.getChannel());
            activity.put("sender_name", log.getSenderName());
            activity.put("received_message", log.getReceivedMessage());
            activity.put("response_type", log.getResponseType());
            activity.put("created_at", log.getCreatedAt());
            activities.add(activity);
        }

        return ResponseEntity.ok(activities);
    }

    // 전체 요약
    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        User user = getCurrentUser();

        Map<String, Object> summary = new HashMap<>();
        summary.put("connected_channels", channelRepository.countByUserIdAndIsActive(user.getId(), true));
        summary.put("active_rules", autoRuleRepository.countByUserIdAndIsActive(user.getId(), true));
        summary.put("pending_reservations", reservationRepository.countByUserIdAndStatus(user.getId(), "PENDING"));

        return ResponseEntity.ok(summary);
    }
}
