package com.autoreplyx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "auto_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "match_type", nullable = false)
    private String matchType = "CONTAINS";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "response_template", nullable = false, columnDefinition = "TEXT")
    private String responseTemplate;

    @Column(name = "include_reservation_link")
    private Boolean includeReservationLink = false;

    @Column(name = "include_estimate_link")
    private Boolean includeEstimateLink = false;

    @Column
    private Integer priority = 100;

    @Column
    private String channel;

    @Column(name = "cooldown_seconds")
    private Integer cooldownSeconds = 60;

    @Column(name = "active_hours_start")
    private LocalTime activeHoursStart;

    @Column(name = "active_hours_end")
    private LocalTime activeHoursEnd;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "trigger_count")
    private Integer triggerCount = 0;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean matches(String message) {
        if (message == null || keywords == null) {
            return false;
        }

        String normalizedMessage = message.trim().toLowerCase();

        String[] keywordList = keywords.split(",");
        for (String keyword : keywordList) {
            String normalizedKeyword = keyword.trim().toLowerCase();
            if (normalizedKeyword.isEmpty()) continue;

            boolean matched = switch (matchType) {
                case "EXACT" -> normalizedMessage.equals(normalizedKeyword);
                case "CONTAINS" -> normalizedMessage.contains(normalizedKeyword);
                case "REGEX" -> {
                    try {
                        yield Pattern.compile(normalizedKeyword, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                                .matcher(normalizedMessage)
                                .find();
                    } catch (Exception e) {
                        yield false;
                    }
                }
                default -> normalizedMessage.contains(normalizedKeyword);
            };

            if (matched) return true;
        }

        return false;
    }

    public boolean isWithinActiveHours() {
        if (activeHoursStart == null || activeHoursEnd == null) {
            return true;
        }

        LocalTime now = LocalTime.now();

        if (activeHoursStart.isAfter(activeHoursEnd)) {
            return now.isAfter(activeHoursStart) || now.isBefore(activeHoursEnd);
        }

        return now.isAfter(activeHoursStart) && now.isBefore(activeHoursEnd);
    }

    public boolean supportsChannel(String targetChannel) {
        if (channel == null || channel.isEmpty() || channel.equalsIgnoreCase("ALL")) {
            return true;
        }
        return channel.equalsIgnoreCase(targetChannel);
    }
}
