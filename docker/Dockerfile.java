FROM gradle:8.4-jdk17 AS builder

WORKDIR /app

# Gradle 의존성 캐싱
COPY backend/java-worker/build.gradle .
COPY backend/java-worker/settings.gradle .
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY backend/java-worker/src ./src
RUN gradle bootJar --no-daemon

# 실행 이미지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 애플리케이션 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
