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
    List<SeatHold> findByTripId(Long tripId);
    List<SeatHold> findByUserId(Long userId);

    @Query("SELECT s FROM SeatHold s WHERE s.expiresAt < CURRENT_TIMESTAMP AND s.status = 'HOLD'")
    List<SeatHold> findExpiredHolds();

    //pendiente verificar si añade un estado usado(markholdasused), y en tocket estadp pendiente para confirmarlo como vendido, o simplemente crearlo al confirmar
    @Modifying
    @Query("""
    UPDATE SeatHold h
    SET h.status = 'USED'
    WHERE h.trip.id = :tripId 
      AND h.seatNumber IN :seatNumbers 
      AND h.status = 'HOLD'
""")
    int markHoldsAsUsed(@Param("tripId") Long tripId,
                        @Param("seatNumbers") List<String> seatNumbers);

    // Buscar holds activos para asientos específicos en un trip
    List<SeatHold> findByTripIdAndSeatNumberInAndStatus(
            Long tripId,
            List<String> seatNumbers,
            SeatHold.Status status
    );

    // Buscar holds activos de un usuario en un trip
    List<SeatHold> findByTripIdAndUserIdAndStatus(
            Long tripId,
            Long userId,
            SeatHold.Status status
    );

    // Verificar si existe hold activo para un asiento
    boolean existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
            Long tripId,
            String seatNumber,
            SeatHold.Status status,
            OffsetDateTime now
    );

    // Buscar hold activo para un asiento específico
    @Query("SELECT sh FROM SeatHold sh " +
            "WHERE sh.trip.id = :tripId " +
            "AND sh.seatNumber = :seatNumber " +
            "AND sh.status = 'HOLD' " +
            "AND sh.expiresAt > :now")
    Optional<SeatHold> findActiveHold(
            @Param("tripId") Long tripId,
            @Param("seatNumber") String seatNumber,
            @Param("now") OffsetDateTime now
    );
}
