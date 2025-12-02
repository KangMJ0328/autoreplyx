package com.autoreplyx.repository;

import com.autoreplyx.entity.EstimateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstimateRequestRepository extends JpaRepository<EstimateRequest, Long> {

    Page<EstimateRequest> findByUserId(Long userId, Pageable pageable);

    Page<EstimateRequest> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    Optional<EstimateRequest> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);
}
