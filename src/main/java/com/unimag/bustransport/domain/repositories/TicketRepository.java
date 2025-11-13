package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket,Long> {
    List<Ticket> findByTripId(Long tripId);
    List<Ticket> findByPassengerId(Long passengerId);
    // Buscar tickets por compra
    List<Ticket> findByPurchaseId(Long purchaseId);
    Optional<Ticket> findByQrCode(String qrCode);
    List<Ticket> findByStatus(Ticket.Status status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.trip.id = :tripId AND t.status = com.unimag.bustransport.domain.entities.Ticket.Status.SOLD")
    long countSoldByTrip(@Param("tripId") Long tripId);
    @Query("SELECT COUNT(t) " +
            "FROM Ticket t " +
            "WHERE t.trip.id = :tripId AND t.seatNumber= :seatNumber " +
            "                          AND t.status = (com.unimag.bustransport.domain.entities.Ticket.Status.SOLD) " +
            "                          AND (t.fromStop.order< :toIndex AND t.toStop.order> :fromIndex)")
    long countSeatOcuppyBetweenStops( @Param("tripId") Long tripId,
                                      @Param("fromIndex") Integer fromIndex,
                                      @Param("toIndex") Integer toIndex,
                                      @Param("seatNumber") String seatNumber);

    // Verificar si un asiento ya está vendido en un trip
    boolean existsByTripIdAndSeatNumberAndStatus(
            Long tripId,
            String seatNumber,
            Ticket.Status status
    );

    // Buscar tickets por trip y números de asiento
    List<Ticket> findByTripIdAndSeatNumberIn(Long tripId, List<String> seatNumbers);

    // Buscar tickets vendidos para un tramo específico (verificar solapamiento)
    @Query("SELECT t FROM Ticket t " +
            "WHERE t.trip.id = :tripId " +
            "AND t.seatNumber = :seatNumber " +
            "AND t.status = com.unimag.bustransport.domain.entities.Ticket.Status.SOLD " +
            "AND (" +
            "  (t.fromStop.order <= :fromOrder AND t.toStop.order > :fromOrder) OR " +
            "  (t.fromStop.order < :toOrder AND t.toStop.order >= :toOrder) OR " +
            "  (t.fromStop.order >= :fromOrder AND t.toStop.order <= :toOrder)" +
            ")")
    List<Ticket> findOverlappingTickets(
            @Param("tripId") Long tripId,
            @Param("seatNumber") String seatNumber,
            @Param("fromOrder") Integer fromOrder,
            @Param("toOrder") Integer toOrder
    );

    @Query("SELECT t FROM Ticket t " +
            "WHERE t.status = com.unimag.bustransport.domain.entities.Ticket.Status.PENDING " +
            "AND t.purchase.paymentStatus = com.unimag.bustransport.domain.entities.Purchase.PaymentStatus.PENDING " +
            "AND t.purchase.createdAt < :cutoffTime")
    List<Ticket> findExpiredPendingTickets(@Param("cutoffTime") OffsetDateTime cutoffTime);
}
