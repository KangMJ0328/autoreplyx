package com.autoreplyx.controller;

import com.autoreplyx.entity.Reservation;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationRepository reservationRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int per_page) {
        User user = getCurrentUser();
        List<Reservation> reservations;

        if (status != null && !status.isEmpty() && !status.equals("all")) {
            reservations = reservationRepository.findByUserIdAndStatusOrderByReservationDateDescReservationTimeDesc(
                    user.getId(), status);
        } else {
            reservations = reservationRepository.findByUserIdOrderByReservationDateDescReservationTimeDesc(user.getId());
        }

        // 페이지네이션 적용
        int total = reservations.size();
        int start = (page - 1) * per_page;
        int end = Math.min(start + per_page, total);
        List<Map<String, Object>> pagedData = reservations.stream()
                .skip(start)
                .limit(per_page)
                .map(this::toReservationDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", pagedData);

        Map<String, Object> meta = new HashMap<>();
        meta.put("current_page", page);
        meta.put("last_page", Math.max(1, (int) Math.ceil((double) total / per_page)));
        meta.put("per_page", per_page);
        meta.put("total", total);
        response.put("meta", meta);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String newStatus = request.get("status");

        return reservationRepository.findById(id)
                .filter(reservation -> reservation.getUserId().equals(user.getId()))
                .map(reservation -> {
                    reservation.setStatus(newStatus);
                    reservationRepository.save(reservation);
                    return ResponseEntity.ok(toReservationDto(reservation));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String note = request.get("note");

        return reservationRepository.findById(id)
                .filter(reservation -> reservation.getUserId().equals(user.getId()))
                .map(reservation -> {
                    String existingNotes = reservation.getNotes() != null ? reservation.getNotes() : "";
                    reservation.setNotes(existingNotes + "\n" + note);
                    reservationRepository.save(reservation);
                    return ResponseEntity.ok(toReservationDto(reservation));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toReservationDto(Reservation reservation) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", reservation.getId());
        dto.put("customer_name", reservation.getCustomerName());
        dto.put("customer_phone", reservation.getCustomerPhone());
        dto.put("customer_email", reservation.getCustomerEmail());
        dto.put("service_name", reservation.getServiceName());
        dto.put("reservation_date", reservation.getReservationDate());
        dto.put("reservation_time", reservation.getReservationTime());
        dto.put("customer_requests", reservation.getCustomerRequests());
        dto.put("status", reservation.getStatus());
        dto.put("notes", reservation.getNotes());
        dto.put("source_channel", reservation.getSourceChannel());
        dto.put("created_at", reservation.getCreatedAt());
        return dto;
    }
}
