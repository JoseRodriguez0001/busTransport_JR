package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket,Long> {
    List<Ticket> findByTripId(Long tripId);
    List<Ticket> findByPassengerId(Long passengerId);
    List<Ticket> findByPurchaseId(Long purchaseId);
    Optional<Ticket> findByQrCode(String qrCode);
    List<Ticket> findByStatus(String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.trip.id = :tripId AND t.status = 'SOLD'")
    long countSoldByTrip(@Param("tripId") Long tripId);
}
