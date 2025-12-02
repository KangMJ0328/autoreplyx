package com.autoreplyx.service;

import com.autoreplyx.entity.AutoRule;
import com.autoreplyx.entity.User;
import com.autoreplyx.repository.AutoRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleDataService {

    private final AutoRuleRepository autoRuleRepository;

    /**
     * 새 사용자를 위한 샘플 자동응답 규칙 생성
     */
    public void createSampleRulesForUser(User user) {
        List<AutoRule> sampleRules = Arrays.asList(
            // 가격 문의 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("가격 문의 응답")
                .matchType("CONTAINS")
                .keywords("가격,얼마,비용,요금,금액,프라이스,price")
                .responseTemplate("안녕하세요! 😊 가격 문의 주셔서 감사합니다.\n\n" +
                    "정확한 가격은 서비스 종류와 옵션에 따라 달라질 수 있어요.\n" +
                    "자세한 상담을 원하시면 편하게 말씀해 주세요!\n\n" +
                    "빠른 시간 내에 안내 도와드리겠습니다. 🙏")
                .priority(1)
                .channel("ALL")
                .cooldownSeconds(300)
                .isActive(true)
                .triggerCount(0)
                .build(),

            // 영업시간 문의 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("영업시간 안내")
                .matchType("CONTAINS")
                .keywords("영업시간,몇시,언제,오픈,마감,운영시간,영업,시간")
                .responseTemplate("안녕하세요! 영업시간 문의 감사합니다. ⏰\n\n" +
                    "📅 운영 시간\n" +
                    "평일: 오전 10시 ~ 오후 7시\n" +
                    "토요일: 오전 10시 ~ 오후 5시\n" +
                    "일요일/공휴일: 휴무\n\n" +
                    "방문 전 예약하시면 더 빠른 상담이 가능합니다! 😊")
                .priority(2)
                .channel("ALL")
                .cooldownSeconds(300)
                .isActive(true)
                .triggerCount(0)
                .build(),

            // 위치/주소 문의 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("위치 안내")
                .matchType("CONTAINS")
                .keywords("위치,주소,어디,찾아가,오시는길,어디에,장소,위치가")
                .responseTemplate("안녕하세요! 위치 안내해 드릴게요. 📍\n\n" +
                    "저희 매장 주소는 프로필에서 확인하실 수 있습니다.\n" +
                    "네이버 지도나 카카오맵에서 검색하시면 쉽게 찾아오실 수 있어요!\n\n" +
                    "주차 공간도 마련되어 있으니 편하게 방문해 주세요. 🚗")
                .priority(3)
                .channel("ALL")
                .cooldownSeconds(300)
                .isActive(true)
                .triggerCount(0)
                .build(),

            // 예약 문의 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("예약 안내")
                .matchType("CONTAINS")
                .keywords("예약,부킹,booking,reserve,신청,접수")
                .responseTemplate("예약 문의 감사합니다! 📝\n\n" +
                    "예약을 원하시면 아래 정보를 알려주세요:\n" +
                    "1️⃣ 원하시는 날짜\n" +
                    "2️⃣ 원하시는 시간\n" +
                    "3️⃣ 연락처\n" +
                    "4️⃣ 요청사항 (있으시면)\n\n" +
                    "확인 후 바로 연락드리겠습니다! 😊")
                .priority(1)
                .channel("ALL")
                .cooldownSeconds(300)
                .isActive(true)
                .includeReservationLink(true)
                .triggerCount(0)
                .build(),

            // 인사 응답 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("인사 응답")
                .matchType("CONTAINS")
                .keywords("안녕,하이,헬로,hello,hi,반가워,처음")
                .responseTemplate("안녕하세요! 😊\n\n" +
                    "방문해 주셔서 감사합니다.\n" +
                    "궁금한 점이 있으시면 편하게 물어봐 주세요!\n\n" +
                    "빠르게 답변 도와드리겠습니다. 💬")
                .priority(10)
                .channel("ALL")
                .cooldownSeconds(600)
                .isActive(true)
                .triggerCount(0)
                .build(),

            // 견적 문의 규칙
            AutoRule.builder()
                .userId(user.getId())
                .name("견적 요청 안내")
                .matchType("CONTAINS")
                .keywords("견적,estimate,quote,상담,문의")
                .responseTemplate("견적 문의 감사합니다! 💼\n\n" +
                    "맞춤 견적을 위해 아래 정보를 알려주시면 빠르게 안내해 드릴게요:\n\n" +
                    "📋 필요한 서비스 종류\n" +
                    "📅 희망 일정\n" +
                    "💰 예산 범위 (있으시면)\n\n" +
                    "상세 견적서를 준비해서 연락드리겠습니다!")
                .priority(2)
                .channel("ALL")
                .cooldownSeconds(300)
                .isActive(true)
                .includeEstimateLink(true)
                .triggerCount(0)
                .build()
        );

        autoRuleRepository.saveAll(sampleRules);
        log.info("Created {} sample rules for user {}", sampleRules.size(), user.getId());
    }
}
