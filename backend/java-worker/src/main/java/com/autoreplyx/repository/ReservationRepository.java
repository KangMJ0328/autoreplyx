package com.autoreplyx.repository;

import com.autoreplyx.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByReservationDateDescReservationTimeDesc(Long userId);

    List<Reservation> findByUserIdAndStatusOrderByReservationDateDescReservationTimeDesc(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);
}
