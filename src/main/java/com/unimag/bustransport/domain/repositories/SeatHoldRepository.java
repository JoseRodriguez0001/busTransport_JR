package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    List<SeatHold> findByUserId(Long userId);

    List<SeatHold> findByTripIdAndUserIdAndStatusAndExpiresAtAfter(
            Long tripId,
            Long userId,
            SeatHold.Status status,
            OffsetDateTime now
    );

    List<SeatHold> findByTripIdAndStatusAndExpiresAtAfter(
            Long tripId,
            SeatHold.Status status,
            OffsetDateTime now
    );

    List<SeatHold> findByTripIdAndSeatNumberInAndStatus(
            Long tripId,
            List<String> seatNumbers,
            SeatHold.Status status
    );

    boolean existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
            Long tripId,
            String seatNumber,
            SeatHold.Status status,
            OffsetDateTime now
    );

    @Query("SELECT s FROM SeatHold s WHERE s.expiresAt < CURRENT_TIMESTAMP AND s.status = 'HOLD'")
    List<SeatHold> findExpiredHolds();

    @Modifying
    @Query("""
        DELETE FROM SeatHold h
        WHERE h.trip.id = :tripId 
          AND h.seatNumber IN :seatNumbers 
          AND h.user.id = :userId
          AND h.status = 'HOLD'
    """)
    int deleteByTripIdAndSeatNumbersAndUserId(
            @Param("tripId") Long tripId,
            @Param("seatNumbers") List<String> seatNumbers,
            @Param("userId") Long userId
    );
}
