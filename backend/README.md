# AutoReplyX Backend

AI 소셜 자동응답 SaaS 백엔드 서버

## 기술 스택

- **API Gateway**: PHP 8.2 + Laravel 10
- **Worker**: Java 17 + Spring Boot 3.x
- **Database**: MySQL 8.0 / MariaDB 10.6
- **Cache/Queue**: Redis 7.x
- **AI**: OpenAI GPT API

## 디렉토리 구조

```
backend/
├── php-api/              # Laravel API Gateway
│   ├── app/
│   │   ├── Http/
│   │   │   ├── Controllers/
│   │   │   │   ├── AuthController.php
│   │   │   │   ├── WebhookController.php
│   │   │   │   ├── ChannelController.php
│   │   │   │   ├── RuleController.php
│   │   │   │   ├── ReservationController.php
│   │   │   │   ├── LogController.php
│   │   │   │   └── DashboardController.php
│   │   │   └── Middleware/
│   │   │       ├── Authenticate.php
│   │   │       └── CheckSubscription.php
│   │   ├── Models/
│   │   │   ├── User.php
│   │   │   ├── Channel.php
│   │   │   ├── AutoRule.php
│   │   │   ├── MessageLog.php
│   │   │   ├── Reservation.php
│   │   │   └── Subscription.php
│   │   └── Services/
│   │       ├── InstagramService.php
│   │       ├── KakaoService.php
│   │       └── NaverService.php
│   ├── routes/
│   │   └── api.php
│   └── config/
│
├── java-worker/          # Spring Boot Worker
│   ├── src/main/java/com/autoreplyx/
│   │   ├── Application.java
│   │   ├── config/
│   │   │   ├── RedisConfig.java
│   │   │   └── OpenAIConfig.java
│   │   ├── service/
│   │   │   ├── MessageProcessor.java
│   │   │   ├── RuleEngine.java
│   │   │   ├── AIService.java
│   │   │   └── ChannelService.java
│   │   ├── model/
│   │   │   ├── IncomingMessage.java
│   │   │   ├── AutoRule.java
│   │   │   └── MessageLog.java
│   │   └── repository/
│   │       ├── UserRepository.java
│   │       └── RuleRepository.java
│   └── src/main/resources/
│       └── application.yml
│
└── docker/               # Docker 설정 (선택)
    ├── docker-compose.yml
    └── nginx/
        └── default.conf
```

## 빠른 시작

### 1. 환경 요구사항

- PHP 8.2+
- Composer 2.x
- Java 17+
- Gradle 8.x
- MySQL 8.0+ / MariaDB 10.6+
- Redis 7.x
- Node.js 18+ (프론트엔드 빌드용)

### 2. PHP API 설치

```bash
cd php-api

# 의존성 설치
composer install

# 환경 파일 설정
cp .env.example .env
php artisan key:generate

# .env 파일 수정
# DB_*, REDIS_*, INSTAGRAM_*, OPENAI_* 설정

# 마이그레이션 실행
php artisan migrate

# 서버 실행
php artisan serve --port=8000
```

### 3. Java Worker 설치

```bash
cd java-worker

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### 4. Redis 실행

```bash
# Docker 사용
docker run -d -p 6379:6379 redis:7-alpine

# 또는 직접 설치
redis-server
```

## API 엔드포인트

### 인증
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/auth/login | 로그인 |
| POST | /api/auth/register | 회원가입 |
| POST | /api/auth/refresh | 토큰 갱신 |
| POST | /api/auth/logout | 로그아웃 |

### 웹훅
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/webhook/instagram | 웹훅 검증 |
| POST | /api/webhook/instagram | 메시지 수신 |

### 채널
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/channels | 연동 채널 목록 |
| POST | /api/channels/instagram/connect | Instagram 연동 |
| DELETE | /api/channels/{channel} | 연동 해제 |

### 규칙
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/rules | 규칙 목록 |
| POST | /api/rules | 규칙 생성 |
| PUT | /api/rules/{id} | 규칙 수정 |
| DELETE | /api/rules/{id} | 규칙 삭제 |
| POST | /api/rules/test | 규칙 테스트 |

### 예약
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/reservations | 예약 목록 |
| PATCH | /api/reservations/{id}/status | 상태 변경 |

### 로그
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/logs | 메시지 로그 |
| GET | /api/logs/export | CSV 내보내기 |

### 대시보드
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/dashboard/stats | 통계 조회 |

## 환경변수 설정

### PHP API (.env)

```env
# App
APP_NAME=AutoReplyX
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.autoreplyx.com

# Database
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=autoreplyx
DB_USERNAME=autoreplyx_user
DB_PASSWORD=secure_password

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379

# Instagram
INSTAGRAM_APP_ID=your_app_id
INSTAGRAM_APP_SECRET=your_app_secret
INSTAGRAM_VERIFY_TOKEN=your_verify_token

# OpenAI
OPENAI_API_KEY=sk-xxxxx

# JWT
JWT_SECRET=your_jwt_secret
JWT_TTL=60
```

### Java Worker (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/autoreplyx
    username: autoreplyx_user
    password: secure_password
  redis:
    host: localhost
    port: 6379

openai:
  api-key: sk-xxxxx
  model: gpt-3.5-turbo

instagram:
  api-url: https://graph.facebook.com/v18.0
```

## 메시지 처리 플로우

```
1. Instagram 웹훅 수신 (PHP)
   ↓
2. 서명 검증 → Redis 큐 푸시 → 200 OK 반환
   ↓
3. Java Worker가 큐에서 메시지 소비
   ↓
4. 규칙엔진 실행
   ├─ 매칭됨 → 규칙 응답 생성
   └─ 미매칭 → AI 응답 생성
   ↓
5. Instagram API로 응답 전송
   ↓
6. 로그 저장
```

## 프로덕션 배포

### 1. Nginx 설정

```nginx
server {
    listen 443 ssl http2;
    server_name api.autoreplyx.com;

    root /var/www/autoreplyx/php-api/public;
    index index.php;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.2-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }
}
```

### 2. Supervisor (Java Worker)

```ini
[program:autoreplyx-worker]
command=/usr/bin/java -jar /var/www/autoreplyx/java-worker/build/libs/worker.jar
directory=/var/www/autoreplyx/java-worker
autostart=true
autorestart=true
user=www-data
numprocs=1
```

### 3. Cron 작업

```bash
# 토큰 갱신 체크 (매일)
0 0 * * * cd /var/www/autoreplyx/php-api && php artisan channels:refresh-tokens

# 통계 집계 (매시간)
0 * * * * cd /var/www/autoreplyx/php-api && php artisan stats:aggregate

# 오래된 로그 정리 (매주)
0 0 * * 0 cd /var/www/autoreplyx/php-api && php artisan logs:cleanup --days=90
```

## 테스트

### PHP API 테스트

```bash
cd php-api
php artisan test
```

### Java Worker 테스트

```bash
cd java-worker
./gradlew test
```

## 모니터링

### 로그 위치

- PHP API: `php-api/storage/logs/laravel.log`
- Java Worker: `java-worker/logs/worker.log`

### 헬스 체크

```bash
# API 헬스 체크
curl https://api.autoreplyx.com/health

# Redis 연결 확인
redis-cli ping

# MySQL 연결 확인
mysql -u autoreplyx_user -p -e "SELECT 1"
```

## 문의

문제가 있으시면 이슈를 등록해주세요.
