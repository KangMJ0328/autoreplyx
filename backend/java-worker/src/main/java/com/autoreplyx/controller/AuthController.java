package com.autoreplyx.controller;

import com.autoreplyx.dto.AuthRequest;
import com.autoreplyx.dto.AuthResponse;
import com.autoreplyx.dto.RegisterRequest;
import com.autoreplyx.dto.UserDto;
import com.autoreplyx.entity.Role;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.UserRepository;
import com.autoreplyx.security.JwtService;
import com.autoreplyx.service.SampleDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SampleDataService sampleDataService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 등록된 이메일입니다."));
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .brandName(request.getBrandName())
                .industry(request.getIndustry())
                .reservationSlug(UUID.randomUUID().toString().substring(0, 8))
                .role(Role.USER)
                .aiEnabled(true)
                .aiTone("friendly")
                .build();

        userRepository.save(user);

        // 새 사용자를 위한 샘플 자동응답 규칙 생성
        sampleDataService.createSampleRulesForUser(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.from(user))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.from(user))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");

        try {
            String email = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                String newAccessToken = jwtService.generateToken(user);
                return ResponseEntity.ok(Map.of(
                        "access_token", newAccessToken,
                        "user", UserDto.from(user)
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 토큰입니다."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "토큰 갱신에 실패했습니다."));
    }
}
