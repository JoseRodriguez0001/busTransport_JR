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

    Optional<SeatHold> findByTripIdAndSeatNumberAndUserId(Long tripId, String seatNumber, Long userId);

    List<SeatHold> findByStatusAndExpiresAtBefore(SeatHold.Status status, OffsetDateTime expiresAtBefore);

    List<SeatHold> findByStatus(SeatHold.Status status);
}
