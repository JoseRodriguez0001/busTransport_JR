package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
    // pendiente verificar si hay hold activo para un asiento en un tramo para ver si el asiento está disponible.
}
