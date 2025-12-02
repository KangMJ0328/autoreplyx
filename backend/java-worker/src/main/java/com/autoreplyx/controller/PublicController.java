package com.autoreplyx.controller;

import com.autoreplyx.entity.EstimateRequest;
import com.autoreplyx.entity.Reservation;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.EstimateRequestRepository;
import com.autoreplyx.repository.ReservationRepository;
import com.autoreplyx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final EstimateRequestRepository estimateRepository;

    /**
     * 공개 예약 페이지 정보 조회
     */
    @GetMapping("/reservation/{slug}")
    public ResponseEntity<?> getReservationPage(@PathVariable String slug) {
        return userRepository.findByReservationSlug(slug)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("brand_name", user.getBrandName());
                    response.put("title", user.getBrandName() + " 예약");
                    response.put("description", user.getDescription());

                    // 예약 폼 필드 정의
                    List<Map<String, Object>> fields = List.of(
                            Map.of("name", "customer_name", "label", "이름", "type", "text", "required", true),
                            Map.of("name", "customer_phone", "label", "연락처", "type", "tel", "required", true),
                            Map.of("name", "customer_email", "label", "이메일", "type", "email", "required", false),
                            Map.of("name", "service_name", "label", "서비스", "type", "select", "required", true,
                                    "options", getServiceOptions(user.getIndustry())),
                            Map.of("name", "reservation_date", "label", "날짜", "type", "date", "required", true),
                            Map.of("name", "reservation_time", "label", "시간", "type", "time", "required", true),
                            Map.of("name", "customer_requests", "label", "요청사항", "type", "textarea", "required", false)
                    );
                    response.put("fields", fields);

                    // 예약 가능한 시간대 (예시)
                    List<Map<String, Object>> availableSlots = generateAvailableSlots(user);
                    response.put("available_slots", availableSlots);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 공개 예약 생성
     */
    @PostMapping("/reservation/{slug}")
    public ResponseEntity<?> createReservation(
            @PathVariable String slug,
            @RequestBody CreateReservationRequest request) {

        return userRepository.findByReservationSlug(slug)
                .map(user -> {
                    Reservation reservation = Reservation.builder()
                            .userId(user.getId())
                            .customerName(request.getCustomerName())
                            .customerPhone(request.getPhone())
                            .customerEmail(request.getEmail())
                            .serviceName(request.getService())
                            .reservationDate(LocalDate.parse(request.getDate()))
                            .reservationTime(LocalTime.parse(request.getTime()))
                            .customerRequests(request.getRequests())
                            .status("pending")
                            .sourceChannel("WEB")
                            .build();

                    Reservation saved = reservationRepository.save(reservation);

                    return ResponseEntity.ok(Map.of(
                            "message", "예약이 접수되었습니다.",
                            "reservation_id", saved.getId()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 공개 견적 페이지 정보 조회
     */
    @GetMapping("/estimate/{slug}")
    public ResponseEntity<?> getEstimatePage(@PathVariable String slug) {
        return userRepository.findByReservationSlug(slug)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("brand_name", user.getBrandName());
                    response.put("title", user.getBrandName() + " 견적 요청");
                    response.put("description", user.getDescription());

                    // 견적 폼 필드 정의
                    List<Map<String, Object>> fields = List.of(
                            Map.of("name", "customer_name", "label", "이름", "type", "text", "required", true),
                            Map.of("name", "customer_phone", "label", "연락처", "type", "tel", "required", true),
                            Map.of("name", "customer_email", "label", "이메일", "type", "email", "required", false),
                            Map.of("name", "service_type", "label", "서비스 종류", "type", "select", "required", true,
                                    "options", getServiceOptions(user.getIndustry())),
                            Map.of("name", "details", "label", "상세 내용", "type", "textarea", "required", true),
                            Map.of("name", "budget", "label", "예산", "type", "text", "required", false),
                            Map.of("name", "preferred_date", "label", "희망일", "type", "date", "required", false)
                    );
                    response.put("fields", fields);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 공개 견적 요청 생성
     */
    @PostMapping("/estimate/{slug}")
    public ResponseEntity<?> createEstimate(
            @PathVariable String slug,
            @RequestBody CreateEstimateRequest request) {

        return userRepository.findByReservationSlug(slug)
                .map(user -> {
                    EstimateRequest estimate = EstimateRequest.builder()
                            .userId(user.getId())
                            .customerName(request.getCustomerName())
                            .customerPhone(request.getPhone())
                            .customerEmail(request.getEmail())
                            .serviceType(request.getServiceType())
                            .details(request.getDetails())
                            .budget(request.getBudget())
                            .preferredDate(request.getPreferredDate() != null
                                    ? LocalDate.parse(request.getPreferredDate())
                                    : null)
                            .status("pending")
                            .source("web")
                            .build();

                    EstimateRequest saved = estimateRepository.save(estimate);

                    return ResponseEntity.ok(Map.of(
                            "message", "견적 요청이 접수되었습니다.",
                            "estimate_id", saved.getId()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 업종별 서비스 옵션
     */
    private List<String> getServiceOptions(String industry) {
        return switch (industry != null ? industry : "other") {
            case "cafe" -> List.of("음료", "디저트", "케이크", "단체 예약", "기타");
            case "restaurant" -> List.of("식사", "룸 예약", "단체 예약", "케이터링", "기타");
            case "beauty" -> List.of("커트", "펌", "염색", "네일", "기타");
            case "shopping" -> List.of("상품 문의", "대량 구매", "커스텀 제작", "기타");
            case "freelance" -> List.of("디자인", "개발", "마케팅", "컨설팅", "기타");
            default -> List.of("서비스 A", "서비스 B", "서비스 C", "기타");
        };
    }

    /**
     * 예약 가능한 시간대 생성
     */
    private List<Map<String, Object>> generateAvailableSlots(User user) {
        List<Map<String, Object>> slots = new ArrayList<>();

        // 향후 7일간의 예약 가능 시간대 생성
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 7; i++) {
            LocalDate date = today.plusDays(i);
            List<String> times = List.of(
                    "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00", "18:00"
            );

            slots.add(Map.of(
                    "date", date.toString(),
                    "times", times
            ));
        }

        return slots;
    }

    @lombok.Data
    public static class CreateReservationRequest {
        private String customerName;
        private String phone;
        private String email;
        private String service;
        private String date;
        private String time;
        private String requests;
    }

    @lombok.Data
    public static class CreateEstimateRequest {
        private String customerName;
        private String phone;
        private String email;
        private String serviceType;
        private String details;
        private String budget;
        private String preferredDate;
    }
}
