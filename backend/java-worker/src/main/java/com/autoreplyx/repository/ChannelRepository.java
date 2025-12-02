package com.autoreplyx.repository;

import com.autoreplyx.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByUserId(Long userId);

    Optional<Channel> findByUserIdAndChannelType(Long userId, String channelType);

    Optional<Channel> findByAccountIdAndChannelType(String accountId, String channelType);

    Optional<Channel> findByAccountId(String accountId);

    Optional<Channel> findByIdAndIsActiveTrue(Long id);

    long countByUserIdAndIsActive(Long userId, Boolean isActive);
}
