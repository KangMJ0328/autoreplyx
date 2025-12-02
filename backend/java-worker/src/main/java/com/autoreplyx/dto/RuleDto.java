package com.autoreplyx.dto;

import com.autoreplyx.entity.AutoRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RuleDto {
    private Long id;
    private String name;

    @JsonProperty("match_type")
    private String matchType;

    private String keywords;

    @JsonProperty("response_template")
    private String responseTemplate;

    private Integer priority;
    private String channel;

    @JsonProperty("cooldown_seconds")
    private Integer cooldownSeconds;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("trigger_count")
    private Integer triggerCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static RuleDto from(AutoRule rule) {
        return RuleDto.builder()
                .id(rule.getId())
                .name(rule.getName())
                .matchType(rule.getMatchType())
                .keywords(rule.getKeywords())
                .responseTemplate(rule.getResponseTemplate())
                .priority(rule.getPriority())
                .channel(rule.getChannel())
                .cooldownSeconds(rule.getCooldownSeconds())
                .isActive(rule.getIsActive())
                .triggerCount(rule.getTriggerCount())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
