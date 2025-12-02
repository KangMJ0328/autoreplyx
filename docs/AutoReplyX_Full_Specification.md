# AutoReplyX - AI 소셜 자동응답 SaaS 전체 기획·설계·개발 문서

---

## 1. 프로젝트 개요

### 1.1 서비스명
**AutoReplyX**

### 1.2 서비스 정의
소상공인·셀러·프리랜서를 위한 Instagram DM/댓글 + KakaoTalk + 네이버 톡톡 자동응답 SaaS

### 1.3 핵심 가치
- 반복되는 문의(가격, 위치, 스케줄, 계좌번호, 배송문의 등)를 AI 기반 자동응답으로 즉시 처리
- 예약/견적 페이지 링크 자동 발송으로 매출 전환률 증대
- 소상공인의 시간 절약 및 고객 응대 품질 향상

### 1.4 타겟 사용자
| 사용자 유형 | 특징 | 주요 니즈 |
|------------|------|----------|
| 카페/음식점 | 영업시간, 위치, 메뉴 문의 반복 | 자동 안내 |
| 쇼핑몰/셀러 | 가격, 배송, 재고 문의 | 자동응답 + 주문링크 |
| 미용실/뷰티샵 | 예약 문의 빈번 | 예약폼 연동 |
| 프리랜서 | 견적, 포트폴리오 문의 | 견적폼 연동 |

---

## 2. 핵심 기능 요약 (MVP)

### 2.1 채널 연동
| 기능 | 설명 | 우선순위 |
|------|------|---------|
| Instagram Graph API | 웹훅 기반 DM/댓글 수신 | P0 (MVP) |
| Kakao 비즈메시지 | 채널 챗봇 API 연동 | P1 |
| 네이버 톡톡 | 스마트스토어 톡톡 메시지 API | P1 |
| 연동 상태 표시 | 토큰, 만료일, 웹훅 상태 | P0 |

### 2.2 자동응답 엔진
| 기능 | 설명 | 우선순위 |
|------|------|---------|
| 키워드 기반 규칙 | 정확일치/포함일치/정규식 | P0 |
| AI 자동생성 응답 | GPT/Claude 기반 | P0 |
| 시간대별 자동응답 | 운영시간 외 메시지 처리 | P1 |
| 응답 ON/OFF 스위치 | 전체/채널별/규칙별 제어 | P0 |
| 쿨다운 설정 | 중복 응답 방지 (기본 1시간) | P0 |

### 2.3 FAQ 템플릿 AI 자동생성
- "내 비즈니스 설명" 입력 → 가격/위치/업체소개/문의유형 자동 생성
- 업종별 템플릿 제공

### 2.4 예약·견적 페이지
| 기능 | 설명 |
|------|------|
| 시간 슬롯 기반 예약 캘린더 | 날짜/시간 선택 |
| 견적 요청 폼 | 이름/연락처/요청사항 |
| 자동 링크 삽입 | 응답 메시지에 URL 자동 포함 |

### 2.5 대시보드
- 메시지 로그 리스트
- 규칙/AI 응답 작동 이력
- 고객 단위 히스토리
- AI 사용량 표시

### 2.6 요금제
| 플랜 | 가격 | 기능 |
|------|------|------|
| Free | 무료 | 하루 20건 자동응답, Instagram만 |
| Pro | 월 9,900원 | 무제한 + 예약폼 + 네이버/카카오 연동 |
| Agency | 월 29,900원 | 멀티샵 관리 + 화이트라벨 |

---

## 3. 상세 기능 명세서 (FDS)

### 3.1 회원/조직

#### 3.1.1 회원가입
```
필수 필드:
- email: string (unique, 이메일 형식 검증)
- password: string (최소 8자, 영문+숫자 조합)
- brand_name: string (2-50자)
- industry: enum (cafe, shopping, beauty, freelance, restaurant, other)

선택 필드:
- phone: string
- business_hours: string
- address: string
```

#### 3.1.2 로그인
```
지원 방식:
1. 이메일/비밀번호 로그인
2. Google OAuth 2.0
3. Instagram OAuth (채널 연동과 통합)

세션 관리:
- JWT 토큰 발급 (Access Token: 1시간, Refresh Token: 7일)
- HttpOnly Cookie 저장
```

#### 3.1.3 프로필 정보
```typescript
interface UserProfile {
  id: number;
  email: string;
  brand_name: string;
  industry: string;
  business_hours: string;
  address: string;
  description: string;
  contact_email: string;
  contact_phone: string;
  reservation_slug: string;  // autoreply.app/{slug}
  ai_enabled: boolean;
  ai_tone: 'professional' | 'friendly' | 'formal' | 'casual';
  banned_words: string[];
  subscription_plan: 'free' | 'pro' | 'agency';
  subscription_expires_at: Date;
  created_at: Date;
  updated_at: Date;
}
```

---

### 3.2 채널연동 (Instagram 상세)

#### 3.2.1 Instagram Graph API 연동 플로우

```
[연동 프로세스]
1. 사용자가 "Instagram 연동하기" 클릭
2. Facebook Login Dialog 호출
   - 필요 권한: instagram_basic, instagram_manage_messages, pages_messaging
3. Authorization Code 수신
4. Access Token 교환 (Short-lived → Long-lived)
5. 웹훅 등록 (verify_token 검증)
6. channels 테이블에 저장

[토큰 갱신 플로우]
- Long-lived Token: 60일 유효
- 만료 14일 전부터 자동 갱신 시도 (Cron)
- 갱신 실패 시 사용자에게 알림
```

#### 3.2.2 웹훅 엔드포인트 정의

```php
// POST /api/webhook/instagram
// 웹훅 수신 처리

Request Headers:
- X-Hub-Signature-256: HMAC SHA256 서명

Request Body (메시지 수신):
{
  "object": "instagram",
  "entry": [
    {
      "id": "INSTAGRAM_BUSINESS_ACCOUNT_ID",
      "time": 1699999999,
      "messaging": [
        {
          "sender": {"id": "SENDER_IGSID"},
          "recipient": {"id": "RECIPIENT_IGSID"},
          "timestamp": 1699999999000,
          "message": {
            "mid": "MESSAGE_ID",
            "text": "가격이 어떻게 되나요?"
          }
        }
      ]
    }
  ]
}

Request Body (댓글 수신):
{
  "object": "instagram",
  "entry": [
    {
      "id": "INSTAGRAM_BUSINESS_ACCOUNT_ID",
      "time": 1699999999,
      "changes": [
        {
          "field": "comments",
          "value": {
            "id": "COMMENT_ID",
            "text": "예약하고 싶어요",
            "from": {"id": "USER_ID", "username": "user123"},
            "media": {"id": "MEDIA_ID"}
          }
        }
      ]
    }
  ]
}

Request Body (스토리 멘션):
{
  "object": "instagram",
  "entry": [
    {
      "id": "INSTAGRAM_BUSINESS_ACCOUNT_ID",
      "time": 1699999999,
      "changes": [
        {
          "field": "mentions",
          "value": {
            "media_id": "STORY_ID",
            "comment_id": null
          }
        }
      ]
    }
  ]
}
```

#### 3.2.3 웹훅 검증 (GET)

```php
// GET /api/webhook/instagram?hub.mode=subscribe&hub.challenge=CHALLENGE&hub.verify_token=TOKEN

Response: echo $hub_challenge (200 OK)
```

#### 3.2.4 응답 전송 API

```php
// Instagram Send Message API
POST https://graph.facebook.com/v18.0/me/messages

Headers:
- Authorization: Bearer {access_token}
- Content-Type: application/json

Body:
{
  "recipient": {"id": "RECIPIENT_IGSID"},
  "message": {"text": "응답 메시지 내용"}
}

Rate Limits:
- 200 messages per second per app
- 응답 윈도우: 24시간 이내 메시지에만 응답 가능
```

#### 3.2.5 에러 처리 및 로깅

```typescript
interface WebhookLog {
  id: number;
  channel: 'instagram' | 'kakao' | 'naver';
  event_type: 'message' | 'comment' | 'mention' | 'verification';
  raw_payload: JSON;
  processed: boolean;
  error_message: string | null;
  created_at: Date;
}
```

---

### 3.3 자동응답 규칙엔진

#### 3.3.1 규칙 정의 스키마

```typescript
interface AutoRule {
  id: number;
  user_id: number;
  name: string;                    // 규칙 이름
  match_type: 'EXACT' | 'CONTAINS' | 'REGEX';
  keyword: string;                 // 키워드 또는 정규식
  response_text: string;           // 응답 텍스트
  include_reservation_link: boolean;  // 예약 링크 포함 여부
  include_estimate_link: boolean;     // 견적 링크 포함 여부
  priority: number;                // 우선순위 (낮을수록 높음)
  channels: string[];              // 적용 채널 ['instagram', 'kakao', 'naver']
  cooldown_minutes: number;        // 쿨다운 (분)
  active_hours_start: string;      // 활성 시작 시간 (HH:MM)
  active_hours_end: string;        // 활성 종료 시간 (HH:MM)
  active_days: number[];           // 활성 요일 [0=일, 1=월, ..., 6=토]
  is_active: boolean;
  created_at: Date;
  updated_at: Date;
}
```

#### 3.3.2 규칙 매칭 알고리즘

```java
// Java Worker - RuleEngine.java

public class RuleEngine {

    public AutoRule findMatchingRule(String message, String channel, int userId) {
        List<AutoRule> rules = ruleRepository.findActiveRulesByUserAndChannel(userId, channel);

        // 우선순위 순으로 정렬
        rules.sort(Comparator.comparingInt(AutoRule::getPriority));

        for (AutoRule rule : rules) {
            // 시간대 체크
            if (!isWithinActiveHours(rule)) continue;

            // 쿨다운 체크
            if (isInCooldown(rule, userId)) continue;

            // 매칭 타입별 검사
            boolean matched = switch (rule.getMatchType()) {
                case EXACT -> message.equalsIgnoreCase(rule.getKeyword());
                case CONTAINS -> message.toLowerCase().contains(rule.getKeyword().toLowerCase());
                case REGEX -> Pattern.compile(rule.getKeyword(), Pattern.CASE_INSENSITIVE)
                                     .matcher(message).find();
            };

            if (matched) {
                return rule;
            }
        }

        return null; // AI 폴백으로 이동
    }
}
```

#### 3.3.3 우선순위 처리 로직

```
[매칭 우선순위]
1. EXACT 매칭 규칙 (우선순위 번호 순)
2. CONTAINS 매칭 규칙 (우선순위 번호 순)
3. REGEX 매칭 규칙 (우선순위 번호 순)
4. AI 폴백 응답

[충돌 처리]
- 동일 우선순위의 여러 규칙이 매칭될 경우 → 먼저 생성된 규칙 적용
- 로그에 "다중 매칭 발생" 기록
```

#### 3.3.4 쿨다운 처리

```java
// Redis 기반 쿨다운 관리
public boolean isInCooldown(AutoRule rule, String senderId) {
    String key = String.format("cooldown:%d:%s", rule.getId(), senderId);
    return redisTemplate.hasKey(key);
}

public void setCooldown(AutoRule rule, String senderId) {
    String key = String.format("cooldown:%d:%s", rule.getId(), senderId);
    redisTemplate.opsForValue().set(key, "1", rule.getCooldownMinutes(), TimeUnit.MINUTES);
}
```

#### 3.3.5 AI 폴백 처리

```java
public String generateAIResponse(String message, UserProfile user) {
    // 규칙에 매칭되지 않은 경우 AI 응답 생성
    if (!user.isAiEnabled()) {
        return getDefaultFallbackMessage();
    }

    String prompt = buildPrompt(message, user);
    String response = openAIService.generateResponse(prompt);

    // 금지어 필터링
    response = filterBannedWords(response, user.getBannedWords());

    return response;
}
```

---

### 3.4 AI 응답 생성

#### 3.4.1 프롬프트 구조

```json
{
  "system_prompt": "당신은 {brand_name}의 고객 응대 AI 어시스턴트입니다.\n\n비즈니스 정보:\n- 업종: {industry}\n- 영업시간: {business_hours}\n- 주소: {address}\n- 설명: {description}\n\n응답 톤: {ai_tone}\n\n규칙:\n1. 간결하고 친절하게 응답\n2. 150자 이내로 응답\n3. 금지어 사용 금지: {banned_words}\n4. 확실하지 않은 정보는 '확인 후 안내드리겠습니다'로 응답",

  "user_message": "{customer_message}",

  "examples": [
    {"role": "user", "content": "영업시간이 어떻게 되나요?"},
    {"role": "assistant", "content": "안녕하세요! 저희 영업시간은 {business_hours}입니다. 방문 전 참고해주세요 :)"}
  ]
}
```

#### 3.4.2 FAQ 자동생성 프롬프트

```json
{
  "system_prompt": "당신은 소상공인 비즈니스 FAQ를 생성하는 전문가입니다.",

  "user_message": "다음 비즈니스 정보를 바탕으로 FAQ 5개를 생성해주세요:\n\n업종: {industry}\n브랜드명: {brand_name}\n설명: {description}\n\n각 FAQ는 다음 형식으로 작성:\n{\n  \"keyword\": \"키워드\",\n  \"response\": \"응답 텍스트\"\n}"
}
```

#### 3.4.3 AI 응답 톤 설정

```typescript
const TONE_PROMPTS = {
  professional: "전문적이고 신뢰감 있는 톤으로 응답하세요. 존댓말을 사용하고 격식체를 유지하세요.",
  friendly: "친근하고 따뜻한 톤으로 응답하세요. 이모티콘을 적절히 사용해도 좋습니다.",
  formal: "격식을 차린 공손한 톤으로 응답하세요. 높임말을 철저히 사용하세요.",
  casual: "편안하고 캐주얼한 톤으로 응답하세요. 자연스러운 대화체를 사용하세요."
};
```

#### 3.4.4 금지어 필터링

```java
public String filterBannedWords(String response, List<String> bannedWords) {
    String filtered = response;
    for (String word : bannedWords) {
        filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), "***");
    }
    return filtered;
}
```

#### 3.4.5 토큰 비용 절감 전략

```
1. 응답 캐싱
   - 동일 질문(유사도 90% 이상) → 캐시된 응답 반환
   - 캐시 TTL: 24시간

2. 프롬프트 최적화
   - 시스템 프롬프트 최소화
   - 예시 1-2개로 제한

3. 모델 선택
   - 단순 질문: GPT-3.5-turbo (저비용)
   - 복잡한 질문: GPT-4 (고품질)

4. 토큰 제한
   - max_tokens: 200
   - 응답 길이 제한: 150자
```

---

### 3.5 예약/견적 페이지

#### 3.5.1 예약 폼 스키마

```typescript
interface ReservationForm {
  id: number;
  user_id: number;
  slug: string;                    // URL 슬러그
  title: string;                   // 페이지 제목
  description: string;             // 설명
  fields: ReservationField[];      // 폼 필드
  time_slots: TimeSlot[];          // 예약 가능 시간
  notification_email: string;      // 알림 수신 이메일
  notification_sms: string;        // 알림 수신 SMS
  is_active: boolean;
  created_at: Date;
}

interface ReservationField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'phone' | 'date' | 'time' | 'textarea' | 'select';
  required: boolean;
  options?: string[];              // select 타입일 때
}

interface TimeSlot {
  day_of_week: number;             // 0-6 (일-토)
  start_time: string;              // HH:MM
  end_time: string;                // HH:MM
  interval_minutes: number;        // 예약 간격
  max_bookings_per_slot: number;   // 슬롯당 최대 예약 수
}
```

#### 3.5.2 기본 예약 폼 필드

```typescript
const DEFAULT_RESERVATION_FIELDS: ReservationField[] = [
  { name: 'customer_name', label: '이름', type: 'text', required: true },
  { name: 'phone', label: '연락처', type: 'phone', required: true },
  { name: 'email', label: '이메일', type: 'email', required: false },
  { name: 'service', label: '서비스', type: 'select', required: true, options: [] },
  { name: 'date', label: '예약 날짜', type: 'date', required: true },
  { name: 'time', label: '예약 시간', type: 'time', required: true },
  { name: 'requests', label: '요청사항', type: 'textarea', required: false }
];
```

#### 3.5.3 견적 요청 폼 스키마

```typescript
interface EstimateForm {
  id: number;
  user_id: number;
  slug: string;
  title: string;
  description: string;
  fields: EstimateField[];
  notification_email: string;
  is_active: boolean;
  created_at: Date;
}

interface EstimateField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'phone' | 'textarea' | 'file' | 'select' | 'checkbox';
  required: boolean;
  options?: string[];
}
```

#### 3.5.4 알림 Webhook

```php
// 예약 생성 시 알림 발송
class NotificationService {

    public function sendReservationNotification(Reservation $reservation) {
        $user = $reservation->user;

        // 이메일 알림
        if ($user->notification_email) {
            Mail::to($user->notification_email)->send(
                new ReservationNotificationMail($reservation)
            );
        }

        // SMS 알림 (외부 API)
        if ($user->notification_sms) {
            $this->smsService->send(
                $user->notification_sms,
                $this->buildSmsMessage($reservation)
            );
        }

        // Webhook 호출 (외부 시스템 연동용)
        if ($user->webhook_url) {
            Http::post($user->webhook_url, [
                'event' => 'reservation.created',
                'data' => $reservation->toArray()
            ]);
        }
    }
}
```

#### 3.5.5 예약 링크 자동삽입 규칙

```typescript
// 응답 메시지 생성 시
function buildResponseMessage(rule: AutoRule, user: UserProfile): string {
  let message = rule.response_text;

  if (rule.include_reservation_link) {
    const reservationUrl = `https://autoreply.app/${user.reservation_slug}`;
    message += `\n\n📅 예약하기: ${reservationUrl}`;
  }

  if (rule.include_estimate_link) {
    const estimateUrl = `https://autoreply.app/${user.reservation_slug}/estimate`;
    message += `\n\n📝 견적 요청: ${estimateUrl}`;
  }

  return message;
}
```

#### 3.5.6 방문 URL 통계

```typescript
interface PageVisitStats {
  page_type: 'reservation' | 'estimate';
  user_id: number;
  total_visits: number;
  unique_visitors: number;
  submissions: number;
  conversion_rate: number;  // submissions / unique_visitors
  visits_by_date: { date: string; count: number }[];
  referrer_stats: { source: string; count: number }[];
}
```

---

### 3.6 로그/대시보드

#### 3.6.1 메시지 로그 스키마

```typescript
interface MessageLog {
  id: number;
  user_id: number;
  channel: 'instagram' | 'kakao' | 'naver';
  sender_id: string;              // 발신자 플랫폼 ID
  sender_name: string;            // 발신자 표시명
  received_message: string;       // 수신 메시지
  response_message: string;       // 응답 메시지
  response_type: 'rule' | 'ai' | 'manual' | 'none';
  matched_rule_id: number | null;
  ai_tokens_used: number;
  processing_time_ms: number;
  created_at: Date;
}
```

#### 3.6.2 대시보드 통계 API

```typescript
interface DashboardStats {
  // 오늘 통계
  today: {
    total_messages: number;
    rule_responses: number;
    ai_responses: number;
    avg_response_time_ms: number;
  };

  // 이번 달 통계
  monthly: {
    total_messages: number;
    rule_responses: number;
    ai_responses: number;
    ai_tokens_used: number;
    ai_cost_estimate: number;
  };

  // 채널별 통계
  by_channel: {
    channel: string;
    message_count: number;
    response_rate: number;
  }[];

  // 규칙별 통계
  top_rules: {
    rule_id: number;
    rule_name: string;
    trigger_count: number;
  }[];

  // 일별 추이
  daily_trend: {
    date: string;
    message_count: number;
  }[];
}
```

#### 3.6.3 CSV Export (Pro 기능)

```php
class LogExportService {

    public function exportToCsv(int $userId, array $filters): string {
        $logs = MessageLog::query()
            ->where('user_id', $userId)
            ->when($filters['start_date'], fn($q) => $q->where('created_at', '>=', $filters['start_date']))
            ->when($filters['end_date'], fn($q) => $q->where('created_at', '<=', $filters['end_date']))
            ->when($filters['channel'], fn($q) => $q->where('channel', $filters['channel']))
            ->orderBy('created_at', 'desc')
            ->get();

        $csv = Writer::createFromString('');
        $csv->insertOne(['시간', '채널', '발신자', '수신 메시지', '응답 메시지', '응답 타입']);

        foreach ($logs as $log) {
            $csv->insertOne([
                $log->created_at->format('Y-m-d H:i:s'),
                $log->channel,
                $log->sender_name,
                $log->received_message,
                $log->response_message,
                $log->response_type
            ]);
        }

        return $csv->toString();
    }
}
```

---

## 4. 백엔드 아키텍처 설계 (Java + PHP 조합)

### 4.1 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           클라이언트 (React)                              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Nginx (리버스 프록시)                             │
│                    - SSL 종료                                            │
│                    - 로드밸런싱                                          │
│                    - Rate Limiting                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌───────────────────────────────┐   ┌───────────────────────────────┐
│      PHP API Gateway          │   │      정적 파일 서버             │
│      (Laravel 10)             │   │      (React Build)             │
│                               │   │                               │
│   - 웹훅 수신                  │   └───────────────────────────────┘
│   - REST API                  │
│   - 인증/인가                  │
│   - 요금제 체크                │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│      Redis                    │
│   - 메시지 큐                  │
│   - 세션 저장                  │
│   - 쿨다운 캐시                │
│   - AI 응답 캐시               │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│      Java Worker              │
│      (Spring Boot)            │
│                               │
│   - 규칙엔진 실행              │
│   - AI API 호출               │
│   - 응답 전송                  │
│   - 로그 저장                  │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────────────────────┐
│                         MySQL / MariaDB                               │
│                    - 사용자 데이터                                      │
│                    - 규칙 데이터                                        │
│                    - 로그 데이터                                        │
└───────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────┐
│      Cron Server (Java)       │
│                               │
│   - 토큰 갱신                  │
│   - 통계 집계                  │
│   - 알림 발송                  │
│   - 로그 정리                  │
└───────────────────────────────┘
```

### 4.2 컴포넌트별 상세

#### 4.2.1 PHP API Gateway (Laravel)

```
/api
├── /auth
│   ├── POST /login              # 로그인
│   ├── POST /register           # 회원가입
│   ├── POST /refresh            # 토큰 갱신
│   └── POST /logout             # 로그아웃
│
├── /webhook
│   ├── GET  /instagram          # 웹훅 검증
│   ├── POST /instagram          # 인스타 웹훅 수신
│   ├── POST /kakao              # 카카오 웹훅 수신
│   └── POST /naver              # 네이버 웹훅 수신
│
├── /channels
│   ├── GET  /                   # 연동 채널 목록
│   ├── POST /instagram/connect  # 인스타 연동
│   ├── DELETE /instagram        # 인스타 연동 해제
│   └── GET  /{channel}/status   # 연동 상태 확인
│
├── /rules
│   ├── GET  /                   # 규칙 목록
│   ├── POST /                   # 규칙 생성
│   ├── PUT  /{id}               # 규칙 수정
│   ├── DELETE /{id}             # 규칙 삭제
│   └── POST /test               # 규칙 테스트
│
├── /ai
│   ├── POST /generate-faq       # FAQ 자동생성
│   └── POST /test-message       # AI 응답 테스트
│
├── /reservations
│   ├── GET  /                   # 예약 목록
│   ├── POST /                   # 예약 생성 (공개)
│   ├── PUT  /{id}/status        # 예약 상태 변경
│   └── GET  /settings           # 예약폼 설정
│
├── /estimates
│   ├── GET  /                   # 견적 요청 목록
│   └── POST /                   # 견적 요청 생성 (공개)
│
├── /logs
│   ├── GET  /                   # 메시지 로그
│   └── GET  /export             # CSV 내보내기
│
├── /dashboard
│   └── GET  /stats              # 대시보드 통계
│
├── /profile
│   ├── GET  /                   # 프로필 조회
│   └── PUT  /                   # 프로필 수정
│
└── /subscription
    ├── GET  /                   # 구독 정보
    └── POST /upgrade            # 플랜 업그레이드
```

#### 4.2.2 Java Worker (Spring Boot)

```java
// MessageProcessor.java
@Component
public class MessageProcessor {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private AIService aiService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MessageLogRepository logRepository;

    @RabbitListener(queues = "message.queue")
    public void processMessage(IncomingMessage message) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 사용자 정보 조회
            User user = userRepository.findByChannelId(message.getRecipientId());

            // 2. 요금제 체크
            if (!subscriptionService.canProcess(user)) {
                logRepository.save(createLog(message, null, "quota_exceeded"));
                return;
            }

            // 3. 규칙 매칭
            AutoRule matchedRule = ruleEngine.findMatchingRule(
                message.getText(),
                message.getChannel(),
                user.getId()
            );

            String responseText;
            String responseType;
            int tokensUsed = 0;

            if (matchedRule != null) {
                // 규칙 매칭됨
                responseText = buildRuleResponse(matchedRule, user);
                responseType = "rule";

                // 쿨다운 설정
                ruleEngine.setCooldown(matchedRule, message.getSenderId());
            } else {
                // AI 폴백
                AIResponse aiResponse = aiService.generateResponse(message.getText(), user);
                responseText = aiResponse.getText();
                responseType = "ai";
                tokensUsed = aiResponse.getTokensUsed();
            }

            // 4. 응답 전송
            channelService.sendMessage(
                message.getChannel(),
                user.getChannelToken(message.getChannel()),
                message.getSenderId(),
                responseText
            );

            // 5. 로그 저장
            long processingTime = System.currentTimeMillis() - startTime;
            logRepository.save(MessageLog.builder()
                .userId(user.getId())
                .channel(message.getChannel())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .receivedMessage(message.getText())
                .responseMessage(responseText)
                .responseType(responseType)
                .matchedRuleId(matchedRule != null ? matchedRule.getId() : null)
                .aiTokensUsed(tokensUsed)
                .processingTimeMs((int) processingTime)
                .build());

        } catch (Exception e) {
            log.error("Message processing failed", e);
            logRepository.save(createErrorLog(message, e));
        }
    }
}
```

#### 4.2.3 Redis 구조

```
# 메시지 큐
message.queue                           # RabbitMQ/Redis Stream

# 세션
session:{session_id}                    # 사용자 세션 데이터

# 쿨다운
cooldown:{rule_id}:{sender_id}          # TTL = cooldown_minutes

# AI 응답 캐시
ai_cache:{hash(message)}                # TTL = 24시간

# Rate Limiting
rate:{user_id}:{date}                   # 일일 요청 카운트
rate:api:{ip}:{minute}                  # API rate limit

# 채널 토큰 캐시
channel_token:{user_id}:{channel}       # 채널 액세스 토큰
```

### 4.3 메시지 처리 흐름 상세

```
[Step 1] Instagram 웹훅 → PHP API Gateway
         │
         ▼
[Step 2] 서명 검증 (X-Hub-Signature-256)
         │
         ├─ 실패 → 403 반환, 로그 저장
         │
         ▼
[Step 3] 이벤트 타입 판별
         │
         ├─ 메시지 → message.queue로 푸시
         ├─ 댓글 → comment.queue로 푸시
         └─ 멘션 → mention.queue로 푸시
         │
         ▼
[Step 4] 200 OK 즉시 반환 (3초 이내)

         ───────────────────────────────

[Step 5] Java Worker가 큐에서 메시지 소비
         │
         ▼
[Step 6] 사용자 정보 조회 + 요금제 체크
         │
         ├─ 할당량 초과 → 로그만 저장, 종료
         │
         ▼
[Step 7] 규칙엔진 실행
         │
         ├─ 매칭됨 → 규칙 응답 생성
         ├─ 미매칭 + AI 활성 → AI 응답 생성
         └─ 미매칭 + AI 비활성 → 기본 응답 또는 무응답
         │
         ▼
[Step 8] 응답 전송 API 호출
         │
         ├─ 성공 → 로그 저장 (response_type = 'rule'/'ai')
         └─ 실패 → 재시도 큐로 이동 (최대 3회)
         │
         ▼
[Step 9] 쿨다운 설정 (Redis)
         │
         ▼
[Step 10] 완료
```

### 4.4 AI 모듈 설계

#### 4.4.1 AI Service 구조

```java
// AIService.java
@Service
public class AIService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public AIResponse generateResponse(String message, User user) {
        // 1. 캐시 확인
        String cacheKey = "ai_cache:" + DigestUtils.md5Hex(message + user.getId());
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new AIResponse(cached, 0, true);
        }

        // 2. 프롬프트 구성
        ChatRequest request = ChatRequest.builder()
            .model(selectModel(message))
            .messages(List.of(
                new ChatMessage("system", buildSystemPrompt(user)),
                new ChatMessage("user", message)
            ))
            .maxTokens(200)
            .temperature(0.7)
            .build();

        // 3. API 호출
        ChatResponse response = restTemplate.postForObject(
            OPENAI_API_URL,
            request,
            ChatResponse.class
        );

        String responseText = response.getChoices().get(0).getMessage().getContent();

        // 4. 금지어 필터링
        responseText = filterBannedWords(responseText, user.getBannedWords());

        // 5. 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, responseText, 24, TimeUnit.HOURS);

        return new AIResponse(responseText, response.getUsage().getTotalTokens(), false);
    }

    private String selectModel(String message) {
        // 단순 질문은 저비용 모델
        if (message.length() < 50) {
            return "gpt-3.5-turbo";
        }
        return "gpt-4";
    }
}
```

#### 4.4.2 프롬프트 템플릿 JSON

```json
{
  "templates": {
    "customer_service": {
      "system": "당신은 {{brand_name}}의 고객 응대 AI입니다.\n\n[비즈니스 정보]\n- 업종: {{industry}}\n- 영업시간: {{business_hours}}\n- 주소: {{address}}\n- 소개: {{description}}\n\n[응답 규칙]\n1. {{tone}} 톤으로 응답\n2. 150자 이내\n3. 불확실한 정보는 확인 후 안내 약속\n4. 금지어: {{banned_words}}",
      "variables": ["brand_name", "industry", "business_hours", "address", "description", "tone", "banned_words"]
    },
    "faq_generator": {
      "system": "소상공인 비즈니스 FAQ를 생성합니다.",
      "user": "다음 정보로 FAQ {{count}}개 생성:\n\n업종: {{industry}}\n브랜드: {{brand_name}}\n설명: {{description}}\n\n형식:\n{\"keyword\": \"키워드\", \"response\": \"응답\"}",
      "variables": ["count", "industry", "brand_name", "description"]
    }
  }
}
```

### 4.5 예약 시스템 구조 (PHP MVC)

```php
// app/Http/Controllers/ReservationController.php

class ReservationController extends Controller
{
    // 예약 페이지 조회 (공개)
    public function show(string $slug)
    {
        $user = User::where('reservation_slug', $slug)->firstOrFail();
        $form = $user->reservationForm;
        $availableSlots = $this->getAvailableSlots($form);

        return response()->json([
            'brand_name' => $user->brand_name,
            'title' => $form->title,
            'description' => $form->description,
            'fields' => $form->fields,
            'available_slots' => $availableSlots
        ]);
    }

    // 예약 생성 (공개)
    public function store(Request $request, string $slug)
    {
        $user = User::where('reservation_slug', $slug)->firstOrFail();

        $validated = $request->validate([
            'customer_name' => 'required|string|max:100',
            'phone' => 'required|string|max:20',
            'email' => 'nullable|email',
            'service' => 'required|string',
            'date' => 'required|date|after:today',
            'time' => 'required|string',
            'requests' => 'nullable|string|max:500'
        ]);

        // 슬롯 가용성 재확인
        if (!$this->isSlotAvailable($user->id, $validated['date'], $validated['time'])) {
            return response()->json(['error' => '선택한 시간은 이미 예약되었습니다.'], 422);
        }

        $reservation = Reservation::create([
            'user_id' => $user->id,
            'customer_name' => $validated['customer_name'],
            'phone' => $validated['phone'],
            'email' => $validated['email'],
            'service' => $validated['service'],
            'date' => $validated['date'],
            'time' => $validated['time'],
            'requests' => $validated['requests'],
            'status' => 'pending'
        ]);

        // 관리자 알림 발송
        event(new ReservationCreated($reservation));

        return response()->json([
            'message' => '예약이 완료되었습니다.',
            'reservation_id' => $reservation->id
        ], 201);
    }

    // 예약 목록 (관리자)
    public function index(Request $request)
    {
        $user = $request->user();

        $reservations = Reservation::where('user_id', $user->id)
            ->when($request->status, fn($q) => $q->where('status', $request->status))
            ->orderBy('date', 'asc')
            ->orderBy('time', 'asc')
            ->paginate(20);

        return response()->json($reservations);
    }

    // 예약 상태 변경
    public function updateStatus(Request $request, int $id)
    {
        $reservation = Reservation::where('user_id', $request->user()->id)
            ->findOrFail($id);

        $validated = $request->validate([
            'status' => 'required|in:pending,confirmed,cancelled'
        ]);

        $reservation->update(['status' => $validated['status']]);

        // 고객 알림 발송
        if ($validated['status'] === 'confirmed') {
            event(new ReservationConfirmed($reservation));
        }

        return response()->json($reservation);
    }
}
```

---

## 5. DB 설계 (ERD 텍스트 정의)

### 5.1 users 테이블

```sql
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    industry ENUM('cafe', 'shopping', 'beauty', 'freelance', 'restaurant', 'other') NOT NULL,
    business_hours VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    description TEXT NULL,
    contact_email VARCHAR(255) NULL,
    contact_phone VARCHAR(20) NULL,
    reservation_slug VARCHAR(50) UNIQUE,
    ai_enabled BOOLEAN DEFAULT TRUE,
    ai_tone ENUM('professional', 'friendly', 'formal', 'casual') DEFAULT 'friendly',
    banned_words TEXT NULL COMMENT 'JSON array of banned words',
    email_verified_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_reservation_slug (reservation_slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.2 channels 테이블

```sql
CREATE TABLE channels (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    channel_type ENUM('instagram', 'kakao', 'naver') NOT NULL,
    account_id VARCHAR(255) NOT NULL COMMENT 'Platform account ID',
    account_name VARCHAR(255) NULL COMMENT 'Display name (@username)',
    access_token TEXT NOT NULL,
    refresh_token TEXT NULL,
    token_expires_at TIMESTAMP NULL,
    webhook_status ENUM('active', 'inactive', 'error') DEFAULT 'inactive',
    webhook_error_message VARCHAR(500) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_channel (user_id, channel_type),
    INDEX idx_channel_type (channel_type),
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.3 auto_rules 테이블

```sql
CREATE TABLE auto_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    match_type ENUM('EXACT', 'CONTAINS', 'REGEX') NOT NULL DEFAULT 'CONTAINS',
    keyword VARCHAR(500) NOT NULL,
    response_text TEXT NOT NULL,
    include_reservation_link BOOLEAN DEFAULT FALSE,
    include_estimate_link BOOLEAN DEFAULT FALSE,
    priority INT UNSIGNED DEFAULT 100 COMMENT 'Lower = higher priority',
    channels JSON NOT NULL COMMENT '["instagram", "kakao", "naver"]',
    cooldown_minutes INT UNSIGNED DEFAULT 60,
    active_hours_start TIME NULL COMMENT 'NULL = always active',
    active_hours_end TIME NULL,
    active_days JSON NULL COMMENT '[0,1,2,3,4,5,6] = all days',
    is_active BOOLEAN DEFAULT TRUE,
    trigger_count BIGINT UNSIGNED DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_active (user_id, is_active),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.4 ai_templates 테이블

```sql
CREATE TABLE ai_templates (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    template_type ENUM('faq', 'greeting', 'closing', 'custom') NOT NULL,
    keyword VARCHAR(255) NULL,
    prompt_template TEXT NOT NULL,
    response_example TEXT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    usage_count BIGINT UNSIGNED DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_type (user_id, template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.5 message_logs 테이블

```sql
CREATE TABLE message_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    channel ENUM('instagram', 'kakao', 'naver') NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255) NULL,
    received_message TEXT NOT NULL,
    response_message TEXT NULL,
    response_type ENUM('rule', 'ai', 'manual', 'none') NOT NULL,
    matched_rule_id BIGINT UNSIGNED NULL,
    ai_tokens_used INT UNSIGNED DEFAULT 0,
    processing_time_ms INT UNSIGNED NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_rule_id) REFERENCES auto_rules(id) ON DELETE SET NULL,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_channel (channel),
    INDEX idx_response_type (response_type),
    INDEX idx_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 파티셔닝 (대용량 처리용)
-- ALTER TABLE message_logs PARTITION BY RANGE (UNIX_TIMESTAMP(created_at)) (...);
```

### 5.6 reservations 테이블

```sql
CREATE TABLE reservations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NULL,
    service VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    time TIME NOT NULL,
    requests TEXT NULL,
    status ENUM('pending', 'confirmed', 'cancelled', 'completed') DEFAULT 'pending',
    source ENUM('chat', 'web', 'manual') DEFAULT 'web',
    source_channel VARCHAR(50) NULL COMMENT 'instagram, kakao, etc.',
    notes TEXT NULL COMMENT 'Admin notes',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date),
    INDEX idx_status (status),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.7 estimate_requests 테이블

```sql
CREATE TABLE estimate_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NULL,
    service_type VARCHAR(255) NOT NULL,
    details TEXT NOT NULL,
    budget VARCHAR(100) NULL,
    preferred_date DATE NULL,
    attachments JSON NULL COMMENT 'Array of file URLs',
    status ENUM('pending', 'quoted', 'accepted', 'rejected', 'completed') DEFAULT 'pending',
    quoted_amount DECIMAL(12,2) NULL,
    quote_message TEXT NULL,
    source ENUM('chat', 'web', 'manual') DEFAULT 'web',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.8 subscriptions 테이블

```sql
CREATE TABLE subscriptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    plan ENUM('free', 'pro', 'agency') NOT NULL DEFAULT 'free',
    status ENUM('active', 'cancelled', 'expired', 'past_due') DEFAULT 'active',
    price_monthly DECIMAL(10,2) NOT NULL DEFAULT 0,
    daily_message_limit INT NULL COMMENT 'NULL = unlimited',
    features JSON NOT NULL COMMENT 'Feature flags',
    payment_provider VARCHAR(50) NULL COMMENT 'stripe, paddle, etc.',
    payment_subscription_id VARCHAR(255) NULL,
    current_period_start TIMESTAMP NULL,
    current_period_end TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user (user_id),
    INDEX idx_status (status),
    INDEX idx_period_end (current_period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.9 webhook_events 테이블

```sql
CREATE TABLE webhook_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    channel ENUM('instagram', 'kakao', 'naver') NOT NULL,
    event_type VARCHAR(50) NOT NULL COMMENT 'message, comment, mention, verification',
    raw_payload JSON NOT NULL,
    signature VARCHAR(255) NULL,
    signature_valid BOOLEAN NULL,
    processed BOOLEAN DEFAULT FALSE,
    process_attempts INT UNSIGNED DEFAULT 0,
    error_message TEXT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_channel_processed (channel, processed),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 30일 이후 자동 삭제 이벤트
-- CREATE EVENT cleanup_webhook_events
-- ON SCHEDULE EVERY 1 DAY
-- DO DELETE FROM webhook_events WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### 5.10 ERD 관계도

```
users (1) ─────────< channels (N)
  │
  ├─────────────────< auto_rules (N)
  │
  ├─────────────────< ai_templates (N)
  │
  ├─────────────────< message_logs (N)
  │                         │
  │                         └────────> auto_rules (matched_rule_id)
  │
  ├─────────────────< reservations (N)
  │
  ├─────────────────< estimate_requests (N)
  │
  └─────────────────< subscriptions (1)

webhook_events (standalone - 임시 저장용)
```

---

## 6. API 엔드포인트 목록 (Request/Response)

### 6.1 Instagram Webhook

#### GET /api/webhook/instagram (검증)

```http
GET /api/webhook/instagram?hub.mode=subscribe&hub.challenge=1234567890&hub.verify_token=my_verify_token
```

**Response (200 OK)**
```
1234567890
```

#### POST /api/webhook/instagram (수신)

**Request Headers**
```
Content-Type: application/json
X-Hub-Signature-256: sha256=abc123...
```

**Request Body**
```json
{
  "object": "instagram",
  "entry": [
    {
      "id": "17841400000000000",
      "time": 1701234567,
      "messaging": [
        {
          "sender": {"id": "7654321098765432"},
          "recipient": {"id": "17841400000000000"},
          "timestamp": 1701234567000,
          "message": {
            "mid": "m_AbCdEf123456",
            "text": "영업시간이 어떻게 되나요?"
          }
        }
      ]
    }
  ]
}
```

**Response (200 OK)**
```json
{
  "status": "received"
}
```

**Validation Logic**
```php
// 1. Signature 검증
$signature = $request->header('X-Hub-Signature-256');
$payload = $request->getContent();
$expectedSignature = 'sha256=' . hash_hmac('sha256', $payload, config('services.instagram.app_secret'));

if (!hash_equals($expectedSignature, $signature)) {
    return response()->json(['error' => 'Invalid signature'], 403);
}

// 2. Object 타입 검증
if ($request->input('object') !== 'instagram') {
    return response()->json(['error' => 'Invalid object type'], 400);
}
```

---

### 6.2 자동응답 테스트 API

#### POST /api/ai/test-message

**Request Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "message": "가격이 어떻게 되나요?",
  "channel": "instagram"
}
```

**Response (200 OK)**
```json
{
  "matched_rule": {
    "id": 2,
    "name": "가격 문의",
    "match_type": "CONTAINS",
    "keyword": "가격"
  },
  "response_type": "rule",
  "response_text": "가격표를 보내드리겠습니다. 잠시만 기다려주세요.\n\n📅 예약하기: https://autoreply.app/mybrand",
  "would_trigger_cooldown": true
}
```

**Response (AI 응답일 경우)**
```json
{
  "matched_rule": null,
  "response_type": "ai",
  "response_text": "안녕하세요! 가격은 서비스 종류에 따라 다릅니다. 자세한 가격표는 링크를 통해 확인해주세요 :)",
  "ai_tokens_used": 45,
  "cached": false
}
```

---

### 6.3 응답 전송 API

#### POST /api/channel/send-message

**Request Headers**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**
```json
{
  "channel": "instagram",
  "recipient_id": "7654321098765432",
  "message": "안녕하세요! 영업시간은 평일 10:00-22:00입니다."
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "message_id": "m_AbCdEf789012",
  "sent_at": "2024-11-27T14:30:00Z"
}
```

**Response (429 Rate Limit)**
```json
{
  "success": false,
  "error": "rate_limit_exceeded",
  "message": "Instagram API rate limit exceeded",
  "retry_after": 60
}
```

---

### 6.4 예약/견적 페이지 API

#### POST /api/reservation/create (공개 API)

**Request Body**
```json
{
  "slug": "mybrand",
  "customer_name": "김철수",
  "phone": "010-1234-5678",
  "email": "kim@example.com",
  "service": "헤어컷 + 펌",
  "date": "2024-12-01",
  "time": "14:00",
  "requests": "짧게 자르고 자연스러운 펌으로 부탁드립니다."
}
```

**Response (201 Created)**
```json
{
  "success": true,
  "message": "예약이 완료되었습니다.",
  "reservation": {
    "id": 123,
    "customer_name": "김철수",
    "service": "헤어컷 + 펌",
    "date": "2024-12-01",
    "time": "14:00",
    "status": "pending"
  }
}
```

**Response (422 Validation Error)**
```json
{
  "success": false,
  "error": "validation_error",
  "message": "선택한 시간은 이미 예약되었습니다.",
  "errors": {
    "time": ["이 시간대는 이미 예약이 완료되었습니다."]
  }
}
```

#### GET /api/reservations (인증 필요)

**Request Headers**
```
Authorization: Bearer {access_token}
```

**Query Parameters**
```
?status=pending&page=1&per_page=20
```

**Response (200 OK)**
```json
{
  "data": [
    {
      "id": 1,
      "customer_name": "김철수",
      "phone": "010-1234-5678",
      "email": "kim@example.com",
      "service": "헤어컷 + 펌",
      "date": "2024-12-01",
      "time": "14:00",
      "requests": "짧게 자르고 자연스러운 펌으로 부탁드립니다.",
      "status": "pending",
      "created_at": "2024-11-27T10:00:00Z"
    }
  ],
  "meta": {
    "current_page": 1,
    "last_page": 5,
    "per_page": 20,
    "total": 95
  }
}
```

---

### 6.5 기타 주요 API

#### POST /api/auth/login

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def50200..."
}
```

#### GET /api/dashboard/stats

**Response (200 OK)**
```json
{
  "today": {
    "total_messages": 45,
    "rule_responses": 32,
    "ai_responses": 13,
    "avg_response_time_ms": 234
  },
  "monthly": {
    "total_messages": 1234,
    "rule_responses": 856,
    "ai_responses": 378,
    "ai_tokens_used": 45000,
    "ai_cost_estimate": 0.90
  },
  "by_channel": [
    {"channel": "instagram", "message_count": 800, "response_rate": 98.5},
    {"channel": "kakao", "message_count": 300, "response_rate": 97.2},
    {"channel": "naver", "message_count": 134, "response_rate": 99.1}
  ],
  "top_rules": [
    {"rule_id": 1, "rule_name": "영업시간 안내", "trigger_count": 234},
    {"rule_id": 2, "rule_name": "가격 문의", "trigger_count": 189}
  ],
  "daily_trend": [
    {"date": "2024-11-21", "message_count": 38},
    {"date": "2024-11-22", "message_count": 42}
  ]
}
```

#### POST /api/rules

**Request Body**
```json
{
  "name": "영업시간 안내",
  "match_type": "CONTAINS",
  "keyword": "영업시간",
  "response_text": "영업시간은 평일 10:00-22:00, 주말 11:00-23:00입니다.",
  "include_reservation_link": true,
  "priority": 1,
  "channels": ["instagram", "kakao"],
  "cooldown_minutes": 60,
  "is_active": true
}
```

**Response (201 Created)**
```json
{
  "id": 5,
  "name": "영업시간 안내",
  "match_type": "CONTAINS",
  "keyword": "영업시간",
  "response_text": "영업시간은 평일 10:00-22:00, 주말 11:00-23:00입니다.",
  "include_reservation_link": true,
  "priority": 1,
  "channels": ["instagram", "kakao"],
  "cooldown_minutes": 60,
  "is_active": true,
  "trigger_count": 0,
  "created_at": "2024-11-27T14:30:00Z"
}
```

---

## 7. 2주 개발 로드맵 (1인 기준)

### 1주차: 핵심 기능 구현 (Instagram → 규칙엔진 → 응답)

| Day | 목표 | 상세 작업 |
|-----|------|----------|
| **Day 1** | 환경 세팅 | - 서버 환경 구성 (PHP 8.2, Java 17, MySQL 8)<br>- Laravel 프로젝트 생성<br>- Spring Boot 프로젝트 생성<br>- DB 스키마 생성 (users, channels, auto_rules, message_logs)<br>- Redis 설치 및 연결 테스트 |
| **Day 2** | 인증 시스템 | - 회원가입/로그인 API 구현<br>- JWT 토큰 발급/검증<br>- 프론트엔드 로그인 연동<br>- 세션 미들웨어 구현 |
| **Day 3** | Instagram 웹훅 | - Meta 개발자 앱 등록<br>- 웹훅 검증 엔드포인트 (GET)<br>- 웹훅 수신 엔드포인트 (POST)<br>- 서명 검증 로직<br>- webhook_events 테이블 저장 |
| **Day 4** | 메시지 큐 구현 | - Redis Stream 또는 RabbitMQ 설정<br>- PHP에서 메시지 푸시 로직<br>- Java Worker 기본 구조<br>- 메시지 소비 테스트 |
| **Day 5** | 규칙엔진 구현 | - auto_rules CRUD API<br>- RuleEngine 클래스 (Java)<br>- 매칭 로직 (EXACT, CONTAINS, REGEX)<br>- 우선순위 처리<br>- 프론트엔드 규칙 관리 UI 연동 |
| **Day 6** | 응답 전송 구현 | - Instagram Send Message API 연동<br>- 채널별 응답 전송 서비스<br>- 쿨다운 처리 (Redis)<br>- 에러 핸들링 및 재시도 로직 |
| **Day 7** | E2E 테스트 & 디버깅 | - 전체 플로우 테스트<br>- 로그 저장 확인<br>- 응답 시간 최적화<br>- 버그 수정 |

### 2주차: 확장 기능 및 런칭 준비

| Day | 목표 | 상세 작업 |
|-----|------|----------|
| **Day 8** | AI 응답 구현 | - OpenAI API 연동<br>- 프롬프트 템플릿 구현<br>- AI 폴백 로직<br>- 응답 캐싱 (Redis)<br>- 금지어 필터링 |
| **Day 9** | 예약 시스템 | - 예약폼 스키마 구현<br>- 공개 예약 페이지 API<br>- 예약 생성/목록/상태 변경<br>- 프론트엔드 예약관리 연동 |
| **Day 10** | 대시보드 구현 | - 통계 집계 쿼리<br>- 대시보드 API<br>- 메시지 로그 API<br>- 프론트엔드 대시보드 연동 |
| **Day 11** | 프로필/설정 | - 프로필 CRUD API<br>- 비즈니스 정보 저장<br>- AI 설정 (톤, 금지어)<br>- 예약 슬러그 설정 |
| **Day 12** | 요금제 구현 | - subscriptions 테이블 활용<br>- 일일 할당량 체크 로직<br>- 플랜별 기능 제한<br>- 업그레이드 페이지 (결제 연동은 추후) |
| **Day 13** | 테스트 & 보안 | - 전체 API 테스트<br>- Rate Limiting 적용<br>- 입력값 검증 강화<br>- XSS/SQL Injection 방지 확인<br>- 로그 정리 |
| **Day 14** | 배포 & 런칭 | - 프로덕션 서버 세팅<br>- SSL 인증서 설정<br>- 도메인 연결<br>- 모니터링 설정<br>- 실사용 테스트<br>- 문서 정리 |

---

## 8. QA 체크리스트

### 8.1 채널 연동 테스트

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| 인스타 연동 | OAuth 플로우 완료 | 토큰 저장, 연동 상태 표시 | ☐ |
| 웹훅 검증 | GET 요청 시 challenge 반환 | 200 OK + challenge 값 | ☐ |
| 토큰 만료일 | 60일 이내 토큰 갱신 | 자동 갱신 또는 알림 | ☐ |
| 연동 해제 | 연동 해제 버튼 클릭 | 토큰 삭제, 상태 변경 | ☐ |

### 8.2 규칙엔진 응답 정확도

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| EXACT 매칭 | "가격" 입력 (규칙: "가격") | 매칭 성공 | ☐ |
| EXACT 불일치 | "가격표" 입력 (규칙: "가격") | 매칭 실패 | ☐ |
| CONTAINS 매칭 | "가격이 얼마예요?" 입력 | 매칭 성공 | ☐ |
| REGEX 매칭 | "예약.*하고싶어요" 패턴 | 매칭 성공 | ☐ |
| 우선순위 | 동일 키워드, 다른 우선순위 | 높은 우선순위 규칙 적용 | ☐ |
| 링크 삽입 | include_reservation_link = true | URL 포함된 응답 | ☐ |

### 8.3 AI 응답 오류 처리

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| AI 정상 응답 | 규칙 미매칭 메시지 | AI 응답 생성 | ☐ |
| AI 비활성 | ai_enabled = false | 기본 폴백 메시지 | ☐ |
| API 타임아웃 | OpenAI 지연 | 폴백 메시지 반환 | ☐ |
| API 에러 | 401/500 에러 | 에러 로그 + 폴백 | ☐ |
| 금지어 필터 | 금지어 포함 응답 | *** 치환 | ☐ |
| 캐시 히트 | 동일 질문 2회 | 2회차 캐시 응답 | ☐ |

### 8.4 중복 응답 방지

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| 쿨다운 작동 | 60분 내 동일 규칙 트리거 | 2회차 응답 안 함 | ☐ |
| 쿨다운 해제 | 60분 후 동일 규칙 트리거 | 응답 정상 발송 | ☐ |
| 다른 규칙 | 쿨다운 중 다른 규칙 트리거 | 정상 응답 | ☐ |
| 다른 사용자 | 동일 규칙, 다른 sender | 각각 응답 | ☐ |

### 8.5 예약 폼 제출 테스트

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| 정상 제출 | 필수 필드 모두 입력 | 201 Created | ☐ |
| 필수값 누락 | customer_name 누락 | 422 Validation Error | ☐ |
| 중복 예약 | 동일 시간대 재예약 | 422 시간 불가 메시지 | ☐ |
| 과거 날짜 | 어제 날짜 선택 | 422 Validation Error | ☐ |
| 알림 발송 | 예약 생성 시 | 이메일/SMS 발송 확인 | ☐ |

### 8.6 속도/지연 테스트

| 항목 | 테스트 내용 | 기준 | 통과 |
|------|------------|------|------|
| 웹훅 응답 | Instagram 웹훅 수신 → 200 반환 | < 3초 | ☐ |
| 규칙 응답 | 메시지 수신 → 응답 전송 | < 5초 | ☐ |
| AI 응답 | AI 생성 포함 | < 10초 | ☐ |
| 대시보드 로딩 | 통계 API 응답 | < 2초 | ☐ |
| 동시 처리 | 10건 동시 메시지 | 전체 < 30초 | ☐ |

### 8.7 모바일 UI 점검

| 항목 | 테스트 내용 | 예상 결과 | 통과 |
|------|------------|----------|------|
| 반응형 레이아웃 | 375px 너비 | 레이아웃 정상 | ☐ |
| 사이드바 | 모바일에서 햄버거 메뉴 | 토글 동작 | ☐ |
| 테이블 스크롤 | 로그 테이블 가로 스크롤 | 스크롤 가능 | ☐ |
| 버튼 터치 | 버튼 최소 44px | 터치 용이 | ☐ |
| 폼 입력 | 모바일 키보드 | 정상 입력 | ☐ |

---

## 9. 런칭 전략 & 마케팅 플랜

### 9.1 14일 무료체험

```
[무료체험 정책]
- 기간: 회원가입 후 14일
- 기능: Pro 플랜 전체 기능
- 제한: 없음 (무제한 자동응답)
- 만료 후: Free 플랜 자동 전환

[구현]
- 회원가입 시 subscription 생성 (plan: 'pro', trial_ends_at: +14days)
- 로그인 시 trial 남은 일수 표시
- 만료 3일 전 이메일 알림
- 만료 시 자동 다운그레이드
```

### 9.2 인스타 셀러 카페·오픈톡 공략

**타겟 커뮤니티**
| 플랫폼 | 커뮤니티명 | 회원수 | 접근 방식 |
|--------|-----------|--------|----------|
| 네이버 카페 | 인스타마켓 셀러모임 | 15만+ | 후기 게시글 |
| 카카오 오픈톡 | 스마트스토어 셀러방 | 5천+ | 정보 공유 |
| 네이버 카페 | 1인기업 CEO모임 | 10만+ | 도구 소개글 |
| 페이스북 | 소상공인 마케팅 그룹 | 3만+ | 경험 공유 |

**게시물 템플릿**
```
[제목] 인스타 DM 자동응답으로 하루 2시간 아꼈어요 (무료 툴 공유)

안녕하세요, OOO 운영 중인 셀러입니다.

하루에 "가격 얼마예요?", "영업시간 알려주세요" 같은
똑같은 DM 50개씩 받는데... 일일이 답장하느라 정신 없었거든요.

근데 최근에 AutoReplyX라는 자동응답 툴 써보니까 진짜 편해요.
- 키워드 넣어두면 알아서 답장
- AI가 알아서 대답도 해줌
- 예약 링크도 자동으로 보내줌

14일 무료라서 일단 써봤는데, 그냥 계속 쓰고 있어요 ㅎㅎ
월 9900원인데 시간 생각하면 완전 남는 장사...

혹시 DM 응대 때문에 고민이신 분들 한번 써보세요!
(광고 아니고 진짜 좋아서 공유합니다)

[링크]
```

### 9.3 네이버 스마트스토어 커뮤니티 게시물 세트

**게시물 1: 문제 제기**
```
[제목] 스마트스토어 + 인스타 운영하시는 분들, 톡톡 응대 어떻게 하세요?

저 혼자 운영하는데 톡톡이랑 인스타 DM 둘 다 신경 쓰려니까
진짜 정신없네요...

특히 밤에 오는 문의는 다음날 답장하면 이미 다른 데서 샀다고 하고 ㅠㅠ
자동응답 쓰시는 분 있으신가요?
```

**게시물 2: 해결책 공유**
```
[제목] 톡톡+인스타 자동응답 세팅 후기 (feat. AutoReplyX)

지난주에 물어봤던 사람인데요,
이것저것 찾아보다가 AutoReplyX 써봤습니다.

[세팅 과정]
1. 회원가입 (1분)
2. 인스타 연동 (2분)
3. 키워드 규칙 5개 만들기 (10분)

끝!

[1주일 사용 후]
- 자동 응답된 메시지: 156건
- 직접 응대: 12건 (복잡한 문의만)
- 예약 링크 통해 예약: 8건

솔직히 월 9900원 아까울 줄 알았는데,
시급으로 치면 몇 만원 아낀 셈이에요.

14일 무료니까 일단 써보세요!
```

### 9.4 자동응답 세팅 대행 업셀링 패키지

```
[서비스 구성]
패키지명: AutoReplyX 세팅 대행 서비스
가격: 50,000원 (1회성)

포함 내용:
1. 채널 연동 대행 (인스타/카카오/네이버)
2. 비즈니스 분석 → 맞춤 FAQ 10개 작성
3. 자동응답 규칙 10개 세팅
4. 예약/견적 페이지 세팅
5. 30분 사용법 교육 (Zoom)
6. 2주간 카톡 Q&A 지원

[타겟]
- IT 세팅에 어려움을 느끼는 중장년 소상공인
- 바빠서 직접 세팅할 시간이 없는 1인 사업자
- 빠르게 시작하고 싶은 신규 셀러

[프로모션]
- Pro 플랜 3개월 결제 시 세팅 대행 무료
- 첫 100명 한정 30,000원 특가
```

### 9.5 에이전시 제휴 (화이트라벨)

```
[Agency 플랜 상세]

대상: 마케팅 에이전시, 웹에이전시, SNS 관리 대행사

가격: 월 29,900원 (기본 5개 샵 포함)
      + 샵 추가: 개당 월 5,000원

기능:
1. 화이트라벨
   - 커스텀 도메인 (replies.agency.com)
   - 로고 변경
   - 브랜드 컬러 변경

2. 멀티샵 관리
   - 통합 대시보드
   - 샵별 개별 로그인 제공
   - 권한 관리 (Admin/Manager/Viewer)

3. 리셀러 마진
   - 파트너 가격: 월 7,000원/샵
   - 권장 판매가: 월 15,000원/샵
   - 마진: 8,000원/샵

4. 전담 지원
   - 슬랙 채널 지원
   - 우선 기능 개발 요청
   - 공동 마케팅

[제휴 프로세스]
1. 제휴 신청 폼 작성
2. 미팅 (Zoom 30분)
3. 데모 계정 발급
4. 계약서 체결
5. 화이트라벨 세팅 (3영업일)
6. 런칭
```

---

## 10. 24시간 착수 체크리스트

### Phase 1: 개발 환경 (2시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | 서버 세팅 | 가비아/카페24/AWS 서버 1대 임대 (Ubuntu 22.04) | ☐ |
| 2 | PHP 설치 | PHP 8.2, Composer, Laravel 10 | ☐ |
| 3 | Java 설치 | JDK 17, Gradle, Spring Boot 3.x | ☐ |
| 4 | DB 설치 | MySQL 8.0 또는 MariaDB 10.6 | ☐ |
| 5 | Redis 설치 | Redis 7.x, redis-cli 테스트 | ☐ |
| 6 | Nginx 설정 | 리버스 프록시, SSL (Let's Encrypt) | ☐ |

### Phase 2: Meta 개발자 설정 (2시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | Facebook 개발자 계정 | developers.facebook.com 가입 | ☐ |
| 2 | 앱 생성 | Business 타입 앱 생성 | ☐ |
| 3 | Instagram Basic Display | 제품 추가 | ☐ |
| 4 | Instagram Graph API | Messaging 권한 요청 | ☐ |
| 5 | 웹훅 URL 설정 | Callback URL 등록 | ☐ |
| 6 | Verify Token 설정 | 랜덤 토큰 생성 및 저장 | ☐ |

### Phase 3: DB 스키마 생성 (1시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | DB 생성 | `CREATE DATABASE autoreplyx;` | ☐ |
| 2 | users 테이블 | 위 스키마 실행 | ☐ |
| 3 | channels 테이블 | 위 스키마 실행 | ☐ |
| 4 | auto_rules 테이블 | 위 스키마 실행 | ☐ |
| 5 | message_logs 테이블 | 위 스키마 실행 | ☐ |
| 6 | 나머지 테이블 | 위 스키마 실행 | ☐ |

### Phase 4: 기본 코드 작성 (3시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | Laravel 프로젝트 | `laravel new autoreplyx-api` | ☐ |
| 2 | 웹훅 라우트 | `Route::match(['get','post'], '/webhook/instagram')` | ☐ |
| 3 | 웹훅 컨트롤러 | 검증/수신 로직 구현 | ☐ |
| 4 | Spring Boot 프로젝트 | Worker 프로젝트 생성 | ☐ |
| 5 | 메시지 리스너 | Redis/RabbitMQ 리스너 | ☐ |
| 6 | 규칙엔진 기본 | RuleEngine 클래스 | ☐ |

### Phase 5: 테스트 도구 준비 (1시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | Postman | 컬렉션 생성, 웹훅 테스트 환경 | ☐ |
| 2 | ngrok | 로컬 터널링 (개발용 웹훅 테스트) | ☐ |
| 3 | Meta 웹훅 테스터 | 개발자 콘솔에서 테스트 메시지 발송 | ☐ |
| 4 | Redis CLI | 큐 모니터링 | ☐ |
| 5 | MySQL Workbench | 데이터 확인 | ☐ |

### Phase 6: 기본 규칙 생성 (30분)

| 순서 | 규칙명 | 키워드 | 응답 | 완료 |
|------|--------|--------|------|------|
| 1 | 영업시간 | 영업시간 | 영업시간은 [시간]입니다. | ☐ |
| 2 | 위치 | 위치, 주소, 어디 | 저희 위치는 [주소]입니다. | ☐ |
| 3 | 가격 | 가격, 얼마 | 가격표를 보내드리겠습니다. | ☐ |
| 4 | 예약 | 예약 | 예약은 링크를 통해 가능합니다. [URL] | ☐ |
| 5 | 운영시간외 | (시간대 조건) | 현재 운영시간이 아닙니다. 내일 답변드리겠습니다. | ☐ |

### Phase 7: E2E 테스트 (1시간)

| 순서 | 테스트 | 예상 결과 | 완료 |
|------|--------|----------|------|
| 1 | 웹훅 검증 | challenge 반환 | ☐ |
| 2 | 테스트 메시지 수신 | webhook_events 저장 | ☐ |
| 3 | 메시지 큐 푸시 | Redis에 메시지 존재 | ☐ |
| 4 | Worker 소비 | 로그 출력 확인 | ☐ |
| 5 | 규칙 매칭 | matched_rule_id 확인 | ☐ |
| 6 | 응답 전송 | Instagram DM 도착 확인 | ☐ |
| 7 | 로그 저장 | message_logs 레코드 확인 | ☐ |

### Phase 8: 프론트엔드 연결 (1시간)

| 순서 | 작업 | 상세 | 완료 |
|------|------|------|------|
| 1 | API URL 설정 | `.env` 파일에 API 엔드포인트 | ☐ |
| 2 | 로그인 연동 | Login.tsx → API 호출 | ☐ |
| 3 | 대시보드 연동 | Dashboard.tsx → stats API | ☐ |
| 4 | 규칙 연동 | Rules.tsx → CRUD API | ☐ |
| 5 | 로그 연동 | Logs.tsx → logs API | ☐ |
| 6 | 빌드 테스트 | `npm run build` 성공 확인 | ☐ |

---

## 부록 A: 환경변수 설정

### Laravel (.env)

```env
APP_NAME=AutoReplyX
APP_ENV=production
APP_KEY=base64:xxxxx
APP_DEBUG=false
APP_URL=https://api.autoreplyx.com

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=autoreplyx
DB_USERNAME=autoreplyx_user
DB_PASSWORD=secure_password

REDIS_HOST=127.0.0.1
REDIS_PASSWORD=null
REDIS_PORT=6379

# Instagram
INSTAGRAM_APP_ID=123456789
INSTAGRAM_APP_SECRET=abc123def456
INSTAGRAM_VERIFY_TOKEN=my_secure_verify_token

# OpenAI
OPENAI_API_KEY=sk-xxxxx

# JWT
JWT_SECRET=your-jwt-secret
JWT_TTL=60

# Mail
MAIL_MAILER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@autoreplyx.com
MAIL_PASSWORD=app_password
```

### Spring Boot (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/autoreplyx
    username: autoreplyx_user
    password: secure_password

  redis:
    host: localhost
    port: 6379

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

openai:
  api-key: sk-xxxxx
  model: gpt-3.5-turbo
  max-tokens: 200

instagram:
  api-url: https://graph.facebook.com/v18.0
```

---

## 부록 B: Nginx 설정

```nginx
# /etc/nginx/sites-available/autoreplyx

server {
    listen 80;
    server_name api.autoreplyx.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.autoreplyx.com;

    ssl_certificate /etc/letsencrypt/live/api.autoreplyx.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.autoreplyx.com/privkey.pem;

    root /var/www/autoreplyx/public;
    index index.php;

    # API 요청
    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }

    # 웹훅 전용 (빠른 응답)
    location /api/webhook {
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $document_root/index.php;
        include fastcgi_params;

        # 타임아웃 설정
        fastcgi_read_timeout 5s;
    }
}

# 프론트엔드
server {
    listen 443 ssl http2;
    server_name app.autoreplyx.com;

    ssl_certificate /etc/letsencrypt/live/app.autoreplyx.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.autoreplyx.com/privkey.pem;

    root /var/www/autoreplyx-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 부록 C: 프론트엔드 API 연동 코드

### API Client (src/lib/api.ts)

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'https://api.autoreplyx.com/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 인터셉터: 토큰 추가
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 인터셉터: 401 처리
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// API 함수들
export const authAPI = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),
  register: (data: any) =>
    api.post('/auth/register', data),
  logout: () =>
    api.post('/auth/logout'),
};

export const rulesAPI = {
  list: () => api.get('/rules'),
  create: (data: any) => api.post('/rules', data),
  update: (id: number, data: any) => api.put(`/rules/${id}`, data),
  delete: (id: number) => api.delete(`/rules/${id}`),
  test: (message: string, channel: string) =>
    api.post('/ai/test-message', { message, channel }),
};

export const dashboardAPI = {
  stats: () => api.get('/dashboard/stats'),
};

export const logsAPI = {
  list: (params: any) => api.get('/logs', { params }),
  export: (params: any) => api.get('/logs/export', { params, responseType: 'blob' }),
};

export const reservationsAPI = {
  list: (params: any) => api.get('/reservations', { params }),
  updateStatus: (id: number, status: string) =>
    api.put(`/reservations/${id}/status`, { status }),
};

export const profileAPI = {
  get: () => api.get('/profile'),
  update: (data: any) => api.put('/profile', data),
};

export const channelsAPI = {
  list: () => api.get('/channels'),
  connectInstagram: (code: string) =>
    api.post('/channels/instagram/connect', { code }),
  disconnect: (channel: string) =>
    api.delete(`/channels/${channel}`),
};
```

---

**문서 작성 완료**

이 문서는 AutoReplyX 서비스의 전체 기획·설계·개발·런칭에 필요한 모든 정보를 담고 있습니다.
즉시 개발에 착수할 수 있도록 상세하게 작성되었습니다.
