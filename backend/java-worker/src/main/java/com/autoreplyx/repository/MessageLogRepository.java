package com.autoreplyx.repository;

import com.autoreplyx.entity.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    Page<MessageLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<MessageLog> findByUserIdAndChannelOrderByCreatedAtDesc(Long userId, String channel, Pageable pageable);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    long countByUserIdAndResponseTypeAndCreatedAtAfter(Long userId, String responseType, LocalDateTime after);

    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndResponseTypeAndCreatedAtBetween(Long userId, String responseType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT l FROM MessageLog l WHERE l.userId = :userId " +
           "AND (:channel IS NULL OR l.channel = :channel) " +
           "ORDER BY l.createdAt DESC")
    Page<MessageLog> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("channel") String channel,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(l.aiTokensUsed), 0) FROM MessageLog l " +
           "WHERE l.userId = :userId AND l.createdAt BETWEEN :start AND :end")
    Long sumAiTokensUsedByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 일별 메시지 수 (차트용)
    @Query("SELECT FUNCTION('DATE', l.createdAt) as date, COUNT(l) as count " +
           "FROM MessageLog l WHERE l.userId = :userId AND l.createdAt >= :since " +
           "GROUP BY FUNCTION('DATE', l.createdAt) ORDER BY date")
    java.util.List<Object[]> countByUserIdGroupByDate(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // 채널별 메시지 수
    @Query("SELECT l.channel, COUNT(l) FROM MessageLog l " +
           "WHERE l.userId = :userId AND l.createdAt >= :since " +
           "GROUP BY l.channel")
    java.util.List<Object[]> countByUserIdGroupByChannel(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // 응답 타입별 메시지 수
    @Query("SELECT l.responseType, COUNT(l) FROM MessageLog l " +
           "WHERE l.userId = :userId AND l.createdAt >= :since " +
           "GROUP BY l.responseType")
    java.util.List<Object[]> countByUserIdGroupByResponseType(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // 최근 메시지
    java.util.List<MessageLog> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
