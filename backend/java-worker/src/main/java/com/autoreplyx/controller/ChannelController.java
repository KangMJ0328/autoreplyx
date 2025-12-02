package com.autoreplyx.controller;

import com.autoreplyx.entity.Channel;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.ChannelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final ObjectMapper objectMapper;

    // Instagram 설정
    @Value("${instagram.app-id:}")
    private String instagramAppId;

    @Value("${instagram.app-secret:}")
    private String instagramAppSecret;

    @Value("${instagram.redirect-uri:http://localhost:8080/api/channels/instagram/callback}")
    private String instagramRedirectUri;

    // Kakao 설정
    @Value("${kakao.client-id:}")
    private String kakaoClientId;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${kakao.redirect-uri:http://localhost:8080/api/channels/kakao/callback}")
    private String kakaoRedirectUri;

    // Naver 설정
    @Value("${naver.client-id:}")
    private String naverClientId;

    @Value("${naver.client-secret:}")
    private String naverClientSecret;

    @Value("${naver.redirect-uri:http://localhost:8080/api/channels/naver/callback}")
    private String naverRedirectUri;

    @Value("${app.frontend-url:http://localhost:3002}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 사용자 ID와 state를 매핑 (실제 운영에서는 Redis 사용 권장)
    private final Map<String, Long> stateUserMap = new java.util.concurrent.ConcurrentHashMap<>();

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        User user = getCurrentUser();
        List<Channel> channels = channelRepository.findByUserId(user.getId());
        return ResponseEntity.ok(channels.stream().map(this::toChannelDto).collect(Collectors.toList()));
    }

    @PostMapping("/instagram/connect")
    public ResponseEntity<?> connectInstagram() {
        User user = getCurrentUser();
        String state = java.util.UUID.randomUUID().toString();
        stateUserMap.put(state, user.getId());

        String authUrl = String.format(
            "https://api.instagram.com/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user_profile,user_media&response_type=code&state=%s",
            instagramAppId,
            instagramRedirectUri,
            state
        );
        return ResponseEntity.ok(Map.of("auth_url", authUrl));
    }

    @GetMapping("/instagram/callback")
    public ResponseEntity<?> instagramCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        // 에러 처리
        if (error != null) {
            log.error("Instagram OAuth error: {} - {}", error, errorDescription);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=" + error)
                    .build();
        }

        // state로 사용자 ID 가져오기
        Long userId = state != null ? stateUserMap.remove(state) : null;
        if (userId == null) {
            log.error("Instagram OAuth: invalid or expired state");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=invalid_state")
                    .build();
        }

        try {
            // 1. Authorization code를 access token으로 교환
            String tokenUrl = "https://api.instagram.com/oauth/access_token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", instagramAppId);
            params.add("client_secret", instagramAppSecret);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", instagramRedirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            String accessToken = tokenJson.path("access_token").asText();

            // 2. Short-lived token을 long-lived token으로 교환
            String longLivedTokenUrl = String.format(
                    "https://graph.instagram.com/access_token?grant_type=ig_exchange_token&client_secret=%s&access_token=%s",
                    instagramAppSecret, accessToken
            );

            ResponseEntity<String> longLivedResponse = restTemplate.getForEntity(longLivedTokenUrl, String.class);
            JsonNode longLivedJson = objectMapper.readTree(longLivedResponse.getBody());

            String longLivedToken = longLivedJson.path("access_token").asText();
            int expiresIn = longLivedJson.path("expires_in").asInt(5184000); // 기본 60일

            // 3. 사용자 프로필 정보 가져오기
            String profileUrl = String.format(
                    "https://graph.instagram.com/me?fields=id,username&access_token=%s",
                    longLivedToken
            );

            ResponseEntity<String> profileResponse = restTemplate.getForEntity(profileUrl, String.class);
            JsonNode profileJson = objectMapper.readTree(profileResponse.getBody());

            String instagramUserId = profileJson.path("id").asText();
            String username = profileJson.path("username").asText();

            // 4. 채널 저장 또는 업데이트
            Channel channel = channelRepository.findByUserIdAndChannelType(userId, "INSTAGRAM")
                    .orElse(Channel.builder()
                            .userId(userId)
                            .channelType("INSTAGRAM")
                            .build());

            channel.setAccountId(instagramUserId);
            channel.setAccountName(username);
            channel.setAccessToken(longLivedToken);
            channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            channel.setIsActive(true);

            channelRepository.save(channel);

            log.info("Instagram channel connected for user {}: {}", userId, username);

            // 5. 프론트엔드로 리다이렉트
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?success=instagram")
                    .build();

        } catch (Exception e) {
            log.error("Instagram OAuth callback error", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=oauth_failed")
                    .build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> disconnect(@PathVariable Long id) {
        User user = getCurrentUser();

        return channelRepository.findById(id)
                .filter(channel -> channel.getUserId().equals(user.getId()))
                .map(channel -> {
                    channelRepository.delete(channel);
                    return ResponseEntity.ok(Map.of("message", "채널 연동이 해제되었습니다."));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshToken(@PathVariable Long id) {
        User user = getCurrentUser();

        return channelRepository.findById(id)
                .filter(channel -> channel.getUserId().equals(user.getId()))
                .map(channel -> {
                    try {
                        // Instagram Graph API로 토큰 갱신
                        String refreshUrl = String.format(
                                "https://graph.instagram.com/refresh_access_token?grant_type=ig_refresh_token&access_token=%s",
                                channel.getAccessToken()
                        );

                        ResponseEntity<String> response = restTemplate.getForEntity(refreshUrl, String.class);
                        JsonNode json = objectMapper.readTree(response.getBody());

                        String newToken = json.path("access_token").asText();
                        int expiresIn = json.path("expires_in").asInt(5184000);

                        channel.setAccessToken(newToken);
                        channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                        channelRepository.save(channel);

                        log.info("Token refreshed for channel {}", id);
                        return ResponseEntity.ok(toChannelDto(channel));
                    } catch (Exception e) {
                        log.error("Token refresh failed for channel {}", id, e);
                        return ResponseEntity.internalServerError()
                                .body(Map.of("error", "토큰 갱신에 실패했습니다."));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable Long id) {
        User user = getCurrentUser();

        return channelRepository.findById(id)
                .filter(channel -> channel.getUserId().equals(user.getId()))
                .map(channel -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("id", channel.getId());
                    status.put("is_active", channel.getIsActive());
                    status.put("token_valid", channel.getTokenExpiresAt() == null ||
                            channel.getTokenExpiresAt().isAfter(LocalDateTime.now()));
                    status.put("token_expires_at", channel.getTokenExpiresAt());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ 카카오톡 채널 연동 (OAuth) ============

    @PostMapping("/kakao/connect")
    public ResponseEntity<?> connectKakao() {
        User user = getCurrentUser();
        String state = java.util.UUID.randomUUID().toString();
        stateUserMap.put(state, user.getId());

        // 카카오 OAuth 인증 URL
        String authUrl = String.format(
            "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=talk_message,profile_nickname&state=%s",
            kakaoClientId,
            kakaoRedirectUri,
            state
        );
        return ResponseEntity.ok(Map.of("auth_url", authUrl));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        // 에러 처리
        if (error != null) {
            log.error("Kakao OAuth error: {} - {}", error, errorDescription);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=" + error)
                    .build();
        }

        // state로 사용자 ID 가져오기
        Long userId = state != null ? stateUserMap.remove(state) : null;
        if (userId == null) {
            log.error("Kakao OAuth: invalid or expired state");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=invalid_state")
                    .build();
        }

        try {
            // 1. Authorization code를 access token으로 교환
            String tokenUrl = "https://kauth.kakao.com/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", kakaoClientId);
            params.add("client_secret", kakaoClientSecret);
            params.add("redirect_uri", kakaoRedirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            String accessToken = tokenJson.path("access_token").asText();
            String refreshToken = tokenJson.path("refresh_token").asText();
            int expiresIn = tokenJson.path("expires_in").asInt(21600); // 기본 6시간

            // 2. 사용자 정보 가져오기
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    userRequest,
                    String.class
            );
            JsonNode userJson = objectMapper.readTree(userResponse.getBody());

            String kakaoUserId = userJson.path("id").asText();
            String nickname = userJson.path("properties").path("nickname").asText();

            // 3. 채널 저장 또는 업데이트
            Channel channel = channelRepository.findByUserIdAndChannelType(userId, "KAKAO")
                    .orElse(Channel.builder()
                            .userId(userId)
                            .channelType("KAKAO")
                            .build());

            channel.setAccountId(kakaoUserId);
            channel.setAccountName(nickname);
            channel.setAccessToken(accessToken);
            channel.setRefreshToken(refreshToken);
            channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            channel.setIsActive(true);

            channelRepository.save(channel);

            log.info("Kakao channel connected for user {}: {}", userId, nickname);

            // 4. 프론트엔드로 리다이렉트
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?success=kakao")
                    .build();

        } catch (Exception e) {
            log.error("Kakao OAuth callback error", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=oauth_failed")
                    .build();
        }
    }

    // 카카오 토큰 갱신
    @PostMapping("/{id}/kakao/refresh")
    public ResponseEntity<?> refreshKakaoToken(@PathVariable Long id) {
        User user = getCurrentUser();

        return channelRepository.findById(id)
                .filter(channel -> channel.getUserId().equals(user.getId()))
                .filter(channel -> "KAKAO".equals(channel.getChannelType()))
                .map(channel -> {
                    try {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                        params.add("grant_type", "refresh_token");
                        params.add("client_id", kakaoClientId);
                        params.add("refresh_token", channel.getRefreshToken());

                        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

                        ResponseEntity<String> response = restTemplate.postForEntity(
                                "https://kauth.kakao.com/oauth/token",
                                request,
                                String.class
                        );
                        JsonNode json = objectMapper.readTree(response.getBody());

                        String newAccessToken = json.path("access_token").asText();
                        int expiresIn = json.path("expires_in").asInt(21600);

                        // refresh_token이 갱신된 경우 업데이트
                        if (json.has("refresh_token")) {
                            channel.setRefreshToken(json.path("refresh_token").asText());
                        }

                        channel.setAccessToken(newAccessToken);
                        channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                        channelRepository.save(channel);

                        log.info("Kakao token refreshed for channel {}", id);
                        return ResponseEntity.ok(toChannelDto(channel));
                    } catch (Exception e) {
                        log.error("Kakao token refresh failed for channel {}", id, e);
                        return ResponseEntity.internalServerError()
                                .body(Map.of("error", "카카오 토큰 갱신에 실패했습니다."));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ 네이버 톡톡 연동 (OAuth) ============

    @PostMapping("/naver/connect")
    public ResponseEntity<?> connectNaver() {
        User user = getCurrentUser();
        String state = java.util.UUID.randomUUID().toString();
        stateUserMap.put(state, user.getId());

        // 네이버 OAuth 인증 URL
        String authUrl = String.format(
            "https://nid.naver.com/oauth2.0/authorize?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
            naverClientId,
            naverRedirectUri,
            state
        );
        return ResponseEntity.ok(Map.of("auth_url", authUrl));
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<?> naverCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        // 에러 처리
        if (error != null) {
            log.error("Naver OAuth error: {} - {}", error, errorDescription);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=" + error)
                    .build();
        }

        // state로 사용자 ID 가져오기
        Long userId = state != null ? stateUserMap.remove(state) : null;
        if (userId == null) {
            log.error("Naver OAuth: invalid or expired state");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=invalid_state")
                    .build();
        }

        try {
            // 1. Authorization code를 access token으로 교환
            String tokenUrl = String.format(
                    "https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
                    naverClientId, naverClientSecret, code, state
            );

            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            String accessToken = tokenJson.path("access_token").asText();
            String refreshToken = tokenJson.path("refresh_token").asText();
            int expiresIn = tokenJson.path("expires_in").asInt(3600); // 기본 1시간

            // 2. 사용자 정보 가져오기
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    userRequest,
                    String.class
            );
            JsonNode userJson = objectMapper.readTree(userResponse.getBody());

            JsonNode responseNode = userJson.path("response");
            String naverId = responseNode.path("id").asText();
            String nickname = responseNode.path("nickname").asText();
            if (nickname == null || nickname.isEmpty()) {
                nickname = responseNode.path("name").asText();
            }

            // 3. 채널 저장 또는 업데이트
            Channel channel = channelRepository.findByUserIdAndChannelType(userId, "NAVER")
                    .orElse(Channel.builder()
                            .userId(userId)
                            .channelType("NAVER")
                            .build());

            channel.setAccountId(naverId);
            channel.setAccountName(nickname);
            channel.setAccessToken(accessToken);
            channel.setRefreshToken(refreshToken);
            channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            channel.setIsActive(true);

            channelRepository.save(channel);

            log.info("Naver channel connected for user {}: {}", userId, nickname);

            // 4. 프론트엔드로 리다이렉트
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?success=naver")
                    .build();

        } catch (Exception e) {
            log.error("Naver OAuth callback error", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendUrl + "/channels?error=oauth_failed")
                    .build();
        }
    }

    // 네이버 토큰 갱신
    @PostMapping("/{id}/naver/refresh")
    public ResponseEntity<?> refreshNaverToken(@PathVariable Long id) {
        User user = getCurrentUser();

        return channelRepository.findById(id)
                .filter(channel -> channel.getUserId().equals(user.getId()))
                .filter(channel -> "NAVER".equals(channel.getChannelType()))
                .map(channel -> {
                    try {
                        String refreshUrl = String.format(
                                "https://nid.naver.com/oauth2.0/token?grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
                                naverClientId, naverClientSecret, channel.getRefreshToken()
                        );

                        ResponseEntity<String> response = restTemplate.getForEntity(refreshUrl, String.class);
                        JsonNode json = objectMapper.readTree(response.getBody());

                        String newAccessToken = json.path("access_token").asText();
                        int expiresIn = json.path("expires_in").asInt(3600);

                        channel.setAccessToken(newAccessToken);
                        channel.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                        channelRepository.save(channel);

                        log.info("Naver token refreshed for channel {}", id);
                        return ResponseEntity.ok(toChannelDto(channel));
                    } catch (Exception e) {
                        log.error("Naver token refresh failed for channel {}", id, e);
                        return ResponseEntity.internalServerError()
                                .body(Map.of("error", "네이버 토큰 갱신에 실패했습니다."));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ============ 인스타그램 개발환경 Mock 연동 ============

    @PostMapping("/instagram/mock-connect")
    public ResponseEntity<?> mockConnectInstagram(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String username = request.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "인스타그램 사용자명을 입력해주세요."));
        }

        Channel channel = channelRepository.findByUserIdAndChannelType(user.getId(), "INSTAGRAM")
                .orElse(Channel.builder()
                        .userId(user.getId())
                        .channelType("INSTAGRAM")
                        .build());

        channel.setAccountId("mock_ig_" + System.currentTimeMillis());
        channel.setAccountName(username);
        channel.setAccessToken("mock_instagram_token_" + System.currentTimeMillis());
        channel.setTokenExpiresAt(LocalDateTime.now().plusDays(60));
        channel.setIsActive(true);

        channelRepository.save(channel);

        log.info("Instagram mock channel connected for user {}: {}", user.getId(), username);
        return ResponseEntity.ok(Map.of(
                "message", "인스타그램이 연동되었습니다. (개발 모드)",
                "channel", toChannelDto(channel)
        ));
    }

    // ============ 카카오톡 개발환경 Mock 연동 ============

    @PostMapping("/kakao/mock-connect")
    public ResponseEntity<?> mockConnectKakao(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String channelId = request.get("channel_id");

        if (channelId == null || channelId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "카카오톡 채널 ID를 입력해주세요."));
        }

        Channel channel = channelRepository.findByUserIdAndChannelType(user.getId(), "KAKAO")
                .orElse(Channel.builder()
                        .userId(user.getId())
                        .channelType("KAKAO")
                        .build());

        channel.setAccountId("mock_kakao_" + System.currentTimeMillis());
        channel.setAccountName("카카오톡 채널 " + channelId);
        channel.setAccessToken("mock_kakao_token_" + System.currentTimeMillis());
        channel.setTokenExpiresAt(LocalDateTime.now().plusDays(365));
        channel.setIsActive(true);

        channelRepository.save(channel);

        log.info("Kakao mock channel connected for user {}: {}", user.getId(), channelId);
        return ResponseEntity.ok(Map.of(
                "message", "카카오톡이 연동되었습니다. (개발 모드)",
                "channel", toChannelDto(channel)
        ));
    }

    // ============ 네이버 톡톡 개발환경 Mock 연동 ============

    @PostMapping("/naver/mock-connect")
    public ResponseEntity<?> mockConnectNaver(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        String talktalkId = request.get("talktalk_id");

        if (talktalkId == null || talktalkId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "네이버 톡톡 ID를 입력해주세요."));
        }

        Channel channel = channelRepository.findByUserIdAndChannelType(user.getId(), "NAVER")
                .orElse(Channel.builder()
                        .userId(user.getId())
                        .channelType("NAVER")
                        .build());

        channel.setAccountId("mock_naver_" + System.currentTimeMillis());
        channel.setAccountName("네이버 톡톡 " + talktalkId);
        channel.setAccessToken("mock_naver_token_" + System.currentTimeMillis());
        channel.setTokenExpiresAt(LocalDateTime.now().plusDays(365));
        channel.setIsActive(true);

        channelRepository.save(channel);

        log.info("Naver mock channel connected for user {}: {}", user.getId(), talktalkId);
        return ResponseEntity.ok(Map.of(
                "message", "네이버 톡톡이 연동되었습니다. (개발 모드)",
                "channel", toChannelDto(channel)
        ));
    }

    private Map<String, Object> toChannelDto(Channel channel) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", channel.getId());
        dto.put("channel_type", channel.getChannelType());
        dto.put("account_id", channel.getAccountId());
        dto.put("account_name", channel.getAccountName());
        dto.put("is_active", channel.getIsActive());
        dto.put("token_expires_at", channel.getTokenExpiresAt());
        dto.put("created_at", channel.getCreatedAt());
        return dto;
    }
}
