package com.autoreplyx.dto;

import com.autoreplyx.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;

    @JsonProperty("brand_name")
    private String brandName;

    private String industry;

    @JsonProperty("business_hours")
    private String businessHours;

    private String address;
    private String description;

    @JsonProperty("reservation_slug")
    private String reservationSlug;

    @JsonProperty("ai_enabled")
    private Boolean aiEnabled;

    @JsonProperty("ai_tone")
    private String aiTone;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .brandName(user.getBrandName())
                .industry(user.getIndustry())
                .businessHours(user.getBusinessHours())
                .address(user.getAddress())
                .description(user.getDescription())
                .reservationSlug(user.getReservationSlug())
                .aiEnabled(user.getAiEnabled())
                .aiTone(user.getAiTone())
                .build();
    }
}
