package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    List<SeatHold> findByTripId(Long tripId);
    List<SeatHold> findByUserId(Long userId);

    @Query("SELECT s FROM SeatHold s WHERE s.expiresAt < CURRENT_TIMESTAMP AND s.status = 'HOLD'")
    List<SeatHold> findExpiredHolds();
}
