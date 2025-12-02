package com.autoreplyx.controller;

import com.autoreplyx.entity.EstimateRequest;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.EstimateRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/estimates")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimateRequestRepository estimateRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int per_page,
            @RequestParam(required = false) String status) {

        PageRequest pageable = PageRequest.of(
                page - 1,
                per_page,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EstimateRequest> estimates;
        if (status != null && !status.isEmpty()) {
            estimates = estimateRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        } else {
            estimates = estimateRepository.findByUserId(user.getId(), pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", estimates.getContent());
        response.put("meta", Map.of(
                "current_page", estimates.getNumber() + 1,
                "last_page", estimates.getTotalPages(),
                "per_page", estimates.getSize(),
                "total", estimates.getTotalElements()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        return estimateRepository.findByIdAndUserId(id, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/quote")
    public ResponseEntity<?> sendQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody SendQuoteRequest request) {

        return estimateRepository.findByIdAndUserId(id, user.getId())
                .map(estimate -> {
                    estimate.setQuotedAmount(request.getAmount());
                    estimate.setQuoteMessage(request.getMessage());
                    estimate.setStatus("quoted");

                    EstimateRequest saved = estimateRepository.save(estimate);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        return estimateRepository.findByIdAndUserId(id, user.getId())
                .map(estimate -> {
                    estimate.setStatus(request.getStatus());
                    EstimateRequest saved = estimateRepository.save(estimate);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody AddNoteRequest request) {

        return estimateRepository.findByIdAndUserId(id, user.getId())
                .map(estimate -> {
                    String currentNotes = estimate.getAdminNotes();
                    String newNote = java.time.LocalDateTime.now() + ": " + request.getNote();
                    estimate.setAdminNotes(currentNotes != null
                            ? currentNotes + "\n" + newNote
                            : newNote);

                    EstimateRequest saved = estimateRepository.save(estimate);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @lombok.Data
    public static class SendQuoteRequest {
        private BigDecimal amount;
        private String message;
    }

    @lombok.Data
    public static class UpdateStatusRequest {
        private String status;
    }

    @lombok.Data
    public static class AddNoteRequest {
        private String note;
    }
}
