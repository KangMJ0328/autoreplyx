package com.autoreplyx.controller;

import com.autoreplyx.dto.UserDto;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PutMapping
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {

        if (request.getBrandName() != null) {
            user.setBrandName(request.getBrandName());
        }
        if (request.getIndustry() != null) {
            user.setIndustry(request.getIndustry());
        }
        if (request.getBusinessHours() != null) {
            user.setBusinessHours(request.getBusinessHours());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getDescription() != null) {
            user.setDescription(request.getDescription());
        }
        if (request.getReservationSlug() != null) {
            user.setReservationSlug(request.getReservationSlug());
        }
        if (request.getAiEnabled() != null) {
            user.setAiEnabled(request.getAiEnabled());
        }
        if (request.getAiTone() != null) {
            user.setAiTone(request.getAiTone());
        }
        if (request.getBannedWords() != null) {
            user.setBannedWords(request.getBannedWords());
        }

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(UserDto.from(savedUser));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal User user,
            @RequestBody UpdatePasswordRequest request) {

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "현재 비밀번호가 일치하지 않습니다."));
        }

        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "새 비밀번호가 일치하지 않습니다."));
        }

        if (request.getPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "비밀번호는 8자 이상이어야 합니다."));
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal User user,
            @RequestBody DeleteAccountRequest request) {

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "비밀번호가 일치하지 않습니다."));
        }

        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "계정이 삭제되었습니다."));
    }

    @lombok.Data
    public static class UpdateProfileRequest {
        private String brandName;
        private String industry;
        private String businessHours;
        private String address;
        private String description;
        private String reservationSlug;
        private Boolean aiEnabled;
        private String aiTone;
        private String bannedWords;
    }

    @lombok.Data
    public static class UpdatePasswordRequest {
        private String currentPassword;
        private String password;
        private String passwordConfirmation;
    }

    @lombok.Data
    public static class DeleteAccountRequest {
        private String password;
    }
}
