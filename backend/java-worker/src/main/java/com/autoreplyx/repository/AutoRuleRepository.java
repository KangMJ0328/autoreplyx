package com.autoreplyx.repository;

import com.autoreplyx.entity.AutoRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AutoRuleRepository extends JpaRepository<AutoRule, Long> {

    List<AutoRule> findByUserIdOrderByPriorityAsc(Long userId);

    List<AutoRule> findByUserIdAndIsActiveTrueOrderByPriorityAsc(Long userId);

    @Query("SELECT r FROM AutoRule r WHERE r.userId = :userId AND r.isActive = true " +
           "AND (r.channel IS NULL OR r.channel = '' OR r.channel = 'ALL' OR r.channel = :channel) " +
           "ORDER BY r.priority")
    List<AutoRule> findActiveRulesByUserAndChannel(
            @Param("userId") Long userId,
            @Param("channel") String channel
    );

    @Modifying
    @Transactional
    @Query("UPDATE AutoRule r SET r.triggerCount = r.triggerCount + 1, " +
           "r.lastTriggeredAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    void incrementTriggerCount(@Param("id") Long id);

    long countByUserIdAndIsActive(Long userId, Boolean isActive);
}
