-- ============================================
-- AutoReplyX Database Schema
-- AI 소셜 자동응답 SaaS
-- ============================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS autoreplyx
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE autoreplyx;

-- ============================================
-- 1. users 테이블
-- 사용자 기본 정보 및 비즈니스 정보
-- ============================================
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    brand_name VARCHAR(100) NOT NULL COMMENT '브랜드명/사업자명',
    industry ENUM('cafe', 'shopping', 'beauty', 'freelance', 'restaurant', 'other') NOT NULL COMMENT '업종',
    business_hours VARCHAR(255) NULL COMMENT '영업시간 (예: 평일 10:00-22:00)',
    address VARCHAR(500) NULL COMMENT '사업장 주소',
    description TEXT NULL COMMENT '비즈니스 소개',
    contact_email VARCHAR(255) NULL COMMENT '고객 연락용 이메일',
    contact_phone VARCHAR(20) NULL COMMENT '고객 연락용 전화번호',
    reservation_slug VARCHAR(50) UNIQUE COMMENT '예약 페이지 URL 슬러그',
    ai_enabled BOOLEAN DEFAULT TRUE COMMENT 'AI 자동응답 활성화',
    ai_tone ENUM('professional', 'friendly', 'formal', 'casual') DEFAULT 'friendly' COMMENT 'AI 응답 톤',
    banned_words TEXT NULL COMMENT 'AI 금지어 목록 (JSON array)',
    notification_email VARCHAR(255) NULL COMMENT '알림 수신 이메일',
    notification_sms VARCHAR(20) NULL COMMENT '알림 수신 SMS',
    email_verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_reservation_slug (reservation_slug),
    INDEX idx_industry (industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. channels 테이블
-- 연동된 메시징 채널 정보
-- ============================================
CREATE TABLE channels (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    channel_type ENUM('instagram', 'kakao', 'naver') NOT NULL COMMENT '채널 유형',
    account_id VARCHAR(255) NOT NULL COMMENT '플랫폼 계정 ID',
    account_name VARCHAR(255) NULL COMMENT '표시 이름 (@username)',
    access_token TEXT NOT NULL COMMENT '액세스 토큰',
    refresh_token TEXT NULL COMMENT '리프레시 토큰',
    token_expires_at TIMESTAMP NULL COMMENT '토큰 만료일',
    webhook_status ENUM('active', 'inactive', 'error') DEFAULT 'inactive' COMMENT '웹훅 상태',
    webhook_error_message VARCHAR(500) NULL COMMENT '웹훅 에러 메시지',
    page_id VARCHAR(255) NULL COMMENT 'Facebook 페이지 ID (Instagram용)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 상태',
    last_message_at TIMESTAMP NULL COMMENT '마지막 메시지 수신 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_channel (user_id, channel_type),
    INDEX idx_channel_type (channel_type),
    INDEX idx_account_id (account_id),
    INDEX idx_webhook_status (webhook_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. auto_rules 테이블
-- 자동응답 규칙 정의
-- ============================================
CREATE TABLE auto_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '규칙 이름',
    match_type ENUM('EXACT', 'CONTAINS', 'REGEX') NOT NULL DEFAULT 'CONTAINS' COMMENT '매칭 타입',
    keyword VARCHAR(500) NOT NULL COMMENT '키워드 또는 정규식 패턴',
    response_text TEXT NOT NULL COMMENT '응답 텍스트',
    include_reservation_link BOOLEAN DEFAULT FALSE COMMENT '예약 링크 포함 여부',
    include_estimate_link BOOLEAN DEFAULT FALSE COMMENT '견적 링크 포함 여부',
    priority INT UNSIGNED DEFAULT 100 COMMENT '우선순위 (낮을수록 높음)',
    channels JSON NOT NULL COMMENT '적용 채널 ["instagram", "kakao", "naver"]',
    cooldown_minutes INT UNSIGNED DEFAULT 60 COMMENT '쿨다운 시간 (분)',
    active_hours_start TIME NULL COMMENT '활성 시작 시간 (NULL = 항상)',
    active_hours_end TIME NULL COMMENT '활성 종료 시간',
    active_days JSON NULL COMMENT '활성 요일 [0,1,2,3,4,5,6]',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 상태',
    trigger_count BIGINT UNSIGNED DEFAULT 0 COMMENT '트리거 횟수',
    last_triggered_at TIMESTAMP NULL COMMENT '마지막 트리거 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_active (user_id, is_active),
    INDEX idx_priority (priority),
    INDEX idx_match_type (match_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. ai_templates 테이블
-- AI 응답 템플릿
-- ============================================
CREATE TABLE ai_templates (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '템플릿 이름',
    template_type ENUM('faq', 'greeting', 'closing', 'custom') NOT NULL COMMENT '템플릿 유형',
    keyword VARCHAR(255) NULL COMMENT '관련 키워드',
    prompt_template TEXT NOT NULL COMMENT '프롬프트 템플릿',
    response_example TEXT NULL COMMENT '응답 예시',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 상태',
    usage_count BIGINT UNSIGNED DEFAULT 0 COMMENT '사용 횟수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_type (user_id, template_type),
    INDEX idx_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. message_logs 테이블
-- 메시지 수신/응답 로그
-- ============================================
CREATE TABLE message_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    channel ENUM('instagram', 'kakao', 'naver') NOT NULL COMMENT '채널',
    sender_id VARCHAR(255) NOT NULL COMMENT '발신자 플랫폼 ID',
    sender_name VARCHAR(255) NULL COMMENT '발신자 표시명',
    received_message TEXT NOT NULL COMMENT '수신 메시지',
    response_message TEXT NULL COMMENT '응답 메시지',
    response_type ENUM('rule', 'ai', 'manual', 'none') NOT NULL COMMENT '응답 유형',
    matched_rule_id BIGINT UNSIGNED NULL COMMENT '매칭된 규칙 ID',
    ai_tokens_used INT UNSIGNED DEFAULT 0 COMMENT 'AI 토큰 사용량',
    processing_time_ms INT UNSIGNED NULL COMMENT '처리 시간 (ms)',
    error_message VARCHAR(500) NULL COMMENT '에러 메시지',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_rule_id) REFERENCES auto_rules(id) ON DELETE SET NULL,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_channel (channel),
    INDEX idx_response_type (response_type),
    INDEX idx_sender (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 6. reservations 테이블
-- 예약 정보
-- ============================================
CREATE TABLE reservations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(100) NOT NULL COMMENT '고객명',
    phone VARCHAR(20) NOT NULL COMMENT '연락처',
    email VARCHAR(255) NULL COMMENT '이메일',
    service VARCHAR(255) NOT NULL COMMENT '서비스/메뉴',
    date DATE NOT NULL COMMENT '예약 날짜',
    time TIME NOT NULL COMMENT '예약 시간',
    requests TEXT NULL COMMENT '요청사항',
    status ENUM('pending', 'confirmed', 'cancelled', 'completed', 'no_show') DEFAULT 'pending' COMMENT '상태',
    source ENUM('chat', 'web', 'manual') DEFAULT 'web' COMMENT '예약 출처',
    source_channel VARCHAR(50) NULL COMMENT '채팅 채널 (instagram, kakao, etc.)',
    notes TEXT NULL COMMENT '관리자 메모',
    reminder_sent BOOLEAN DEFAULT FALSE COMMENT '리마인더 발송 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date),
    INDEX idx_status (status),
    INDEX idx_phone (phone),
    INDEX idx_date_time (date, time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 7. estimate_requests 테이블
-- 견적 요청 정보
-- ============================================
CREATE TABLE estimate_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(100) NOT NULL COMMENT '고객명',
    phone VARCHAR(20) NOT NULL COMMENT '연락처',
    email VARCHAR(255) NULL COMMENT '이메일',
    service_type VARCHAR(255) NOT NULL COMMENT '서비스 유형',
    details TEXT NOT NULL COMMENT '상세 내용',
    budget VARCHAR(100) NULL COMMENT '예산 범위',
    preferred_date DATE NULL COMMENT '희망 날짜',
    attachments JSON NULL COMMENT '첨부파일 URL 배열',
    status ENUM('pending', 'quoted', 'accepted', 'rejected', 'completed') DEFAULT 'pending' COMMENT '상태',
    quoted_amount DECIMAL(12,2) NULL COMMENT '견적 금액',
    quote_message TEXT NULL COMMENT '견적 메시지',
    source ENUM('chat', 'web', 'manual') DEFAULT 'web' COMMENT '요청 출처',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 8. subscriptions 테이블
-- 구독/요금제 정보
-- ============================================
CREATE TABLE subscriptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    plan ENUM('free', 'pro', 'agency') NOT NULL DEFAULT 'free' COMMENT '요금제',
    status ENUM('active', 'trialing', 'cancelled', 'expired', 'past_due') DEFAULT 'active' COMMENT '상태',
    price_monthly DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '월 요금',
    daily_message_limit INT NULL COMMENT '일일 메시지 제한 (NULL = 무제한)',
    features JSON NOT NULL COMMENT '기능 플래그',
    payment_provider VARCHAR(50) NULL COMMENT '결제 제공자 (stripe, paddle, etc.)',
    payment_subscription_id VARCHAR(255) NULL COMMENT '외부 구독 ID',
    trial_ends_at TIMESTAMP NULL COMMENT '트라이얼 종료일',
    current_period_start TIMESTAMP NULL COMMENT '현재 기간 시작',
    current_period_end TIMESTAMP NULL COMMENT '현재 기간 종료',
    cancelled_at TIMESTAMP NULL COMMENT '취소일',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user (user_id),
    INDEX idx_status (status),
    INDEX idx_period_end (current_period_end),
    INDEX idx_plan (plan)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 9. webhook_events 테이블
-- 웹훅 이벤트 로그 (임시 저장용)
-- ============================================
CREATE TABLE webhook_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    channel ENUM('instagram', 'kakao', 'naver') NOT NULL COMMENT '채널',
    event_type VARCHAR(50) NOT NULL COMMENT '이벤트 유형 (message, comment, mention, verification)',
    raw_payload JSON NOT NULL COMMENT '원본 페이로드',
    signature VARCHAR(255) NULL COMMENT '서명',
    signature_valid BOOLEAN NULL COMMENT '서명 검증 결과',
    processed BOOLEAN DEFAULT FALSE COMMENT '처리 완료 여부',
    process_attempts INT UNSIGNED DEFAULT 0 COMMENT '처리 시도 횟수',
    error_message TEXT NULL COMMENT '에러 메시지',
    processed_at TIMESTAMP NULL COMMENT '처리 완료 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_channel_processed (channel, processed),
    INDEX idx_created (created_at),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 10. reservation_settings 테이블
-- 예약 페이지 설정
-- ============================================
CREATE TABLE reservation_settings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT '예약하기' COMMENT '페이지 제목',
    description TEXT NULL COMMENT '페이지 설명',
    services JSON NULL COMMENT '서비스 목록 [{name, duration, price}]',
    time_slot_duration INT UNSIGNED DEFAULT 60 COMMENT '시간 슬롯 간격 (분)',
    max_bookings_per_slot INT UNSIGNED DEFAULT 1 COMMENT '슬롯당 최대 예약',
    advance_booking_days INT UNSIGNED DEFAULT 30 COMMENT '사전 예약 가능 일수',
    business_hours JSON NULL COMMENT '영업시간 설정 [{day, start, end}]',
    blocked_dates JSON NULL COMMENT '예약 불가 날짜 배열',
    confirmation_message TEXT NULL COMMENT '예약 확인 메시지',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 상태',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 11. daily_usage 테이블
-- 일별 사용량 통계
-- ============================================
CREATE TABLE daily_usage (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    date DATE NOT NULL COMMENT '날짜',
    total_messages INT UNSIGNED DEFAULT 0 COMMENT '총 메시지 수',
    rule_responses INT UNSIGNED DEFAULT 0 COMMENT '규칙 응답 수',
    ai_responses INT UNSIGNED DEFAULT 0 COMMENT 'AI 응답 수',
    ai_tokens_used INT UNSIGNED DEFAULT 0 COMMENT 'AI 토큰 사용량',
    reservations_created INT UNSIGNED DEFAULT 0 COMMENT '생성된 예약 수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 12. customer_profiles 테이블
-- 고객 프로필 (대화 이력용)
-- ============================================
CREATE TABLE customer_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '사업자 ID',
    channel ENUM('instagram', 'kakao', 'naver') NOT NULL COMMENT '채널',
    platform_id VARCHAR(255) NOT NULL COMMENT '플랫폼 고객 ID',
    display_name VARCHAR(255) NULL COMMENT '표시명',
    profile_image_url VARCHAR(500) NULL COMMENT '프로필 이미지',
    first_contact_at TIMESTAMP NOT NULL COMMENT '첫 연락 시간',
    last_contact_at TIMESTAMP NOT NULL COMMENT '마지막 연락 시간',
    total_messages INT UNSIGNED DEFAULT 0 COMMENT '총 메시지 수',
    total_reservations INT UNSIGNED DEFAULT 0 COMMENT '총 예약 수',
    notes TEXT NULL COMMENT '관리자 메모',
    tags JSON NULL COMMENT '태그 배열',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_channel_platform (user_id, channel, platform_id),
    INDEX idx_last_contact (last_contact_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 이벤트: 30일 이후 webhook_events 자동 삭제
-- ============================================
DELIMITER //
CREATE EVENT IF NOT EXISTS cleanup_webhook_events
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    DELETE FROM webhook_events
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END //
DELIMITER ;

-- ============================================
-- 이벤트: 90일 이후 message_logs 아카이브
-- (필요시 별도 아카이브 테이블로 이동)
-- ============================================
-- DELIMITER //
-- CREATE EVENT IF NOT EXISTS archive_message_logs
-- ON SCHEDULE EVERY 1 DAY
-- STARTS CURRENT_TIMESTAMP
-- DO
-- BEGIN
--     -- 아카이브 로직 구현
-- END //
-- DELIMITER ;

-- ============================================
-- 기본 데이터 삽입 (테스트용)
-- ============================================

-- 테스트 사용자
INSERT INTO users (email, password, brand_name, industry, business_hours, address, description, reservation_slug, ai_enabled, ai_tone)
VALUES (
    'test@example.com',
    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: 'password'
    '테스트 카페',
    'cafe',
    '평일 10:00-22:00, 주말 11:00-23:00',
    '서울시 강남구 테헤란로 123',
    '맛있는 커피와 디저트를 제공하는 카페입니다.',
    'testcafe',
    TRUE,
    'friendly'
);

-- 테스트 구독
INSERT INTO subscriptions (user_id, plan, status, price_monthly, daily_message_limit, features, trial_ends_at)
VALUES (
    1,
    'pro',
    'trialing',
    9900,
    NULL,
    '{"instagram": true, "kakao": true, "naver": true, "ai_response": true, "reservation_page": true, "export_csv": true}',
    DATE_ADD(NOW(), INTERVAL 14 DAY)
);

-- 테스트 규칙
INSERT INTO auto_rules (user_id, name, match_type, keyword, response_text, include_reservation_link, priority, channels, cooldown_minutes, is_active)
VALUES
(1, '영업시간 안내', 'CONTAINS', '영업시간', '영업시간은 평일 10:00-22:00, 주말 11:00-23:00입니다. 방문 전 참고해주세요!', FALSE, 1, '["instagram", "kakao", "naver"]', 60, TRUE),
(1, '가격 문의', 'CONTAINS', '가격', '가격표를 보내드리겠습니다. 잠시만 기다려주세요!', FALSE, 2, '["instagram", "kakao"]', 120, TRUE),
(1, '예약 방법', 'CONTAINS', '예약', '예약은 아래 링크를 통해 편하게 하실 수 있습니다!', TRUE, 3, '["instagram", "kakao", "naver"]', 60, TRUE),
(1, '위치 안내', 'REGEX', '(위치|주소|어디|찾아가)', '저희 위치는 서울시 강남구 테헤란로 123입니다. 강남역 3번 출구에서 도보 5분 거리에요!', FALSE, 4, '["instagram", "kakao", "naver"]', 60, TRUE),
(1, '메뉴 문의', 'CONTAINS', '메뉴', '저희 카페의 대표 메뉴는 아메리카노, 카페라떼, 티라미수입니다. 자세한 메뉴는 방문 시 확인해주세요!', FALSE, 5, '["instagram", "kakao"]', 60, TRUE);

-- 예약 설정
INSERT INTO reservation_settings (user_id, title, description, services, time_slot_duration, max_bookings_per_slot, advance_booking_days, business_hours, is_active)
VALUES (
    1,
    '테스트 카페 예약',
    '편안한 분위기에서 좋은 시간 보내세요!',
    '[{"name": "일반석 예약", "duration": 60, "price": 0}, {"name": "룸 예약", "duration": 120, "price": 10000}]',
    30,
    2,
    14,
    '[{"day": 1, "start": "10:00", "end": "22:00"}, {"day": 2, "start": "10:00", "end": "22:00"}, {"day": 3, "start": "10:00", "end": "22:00"}, {"day": 4, "start": "10:00", "end": "22:00"}, {"day": 5, "start": "10:00", "end": "22:00"}, {"day": 6, "start": "11:00", "end": "23:00"}, {"day": 0, "start": "11:00", "end": "23:00"}]',
    TRUE
);

-- ============================================
-- 뷰: 대시보드 통계용
-- ============================================
CREATE OR REPLACE VIEW v_dashboard_stats AS
SELECT
    user_id,
    DATE(created_at) as date,
    COUNT(*) as total_messages,
    SUM(CASE WHEN response_type = 'rule' THEN 1 ELSE 0 END) as rule_responses,
    SUM(CASE WHEN response_type = 'ai' THEN 1 ELSE 0 END) as ai_responses,
    SUM(ai_tokens_used) as ai_tokens_used,
    AVG(processing_time_ms) as avg_processing_time
FROM message_logs
GROUP BY user_id, DATE(created_at);

-- ============================================
-- 뷰: 규칙별 트리거 통계
-- ============================================
CREATE OR REPLACE VIEW v_rule_stats AS
SELECT
    r.id as rule_id,
    r.user_id,
    r.name as rule_name,
    r.keyword,
    r.is_active,
    r.trigger_count,
    COUNT(ml.id) as recent_triggers,
    MAX(ml.created_at) as last_triggered
FROM auto_rules r
LEFT JOIN message_logs ml ON r.id = ml.matched_rule_id
    AND ml.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY r.id, r.user_id, r.name, r.keyword, r.is_active, r.trigger_count;
