package com.unimag.bustransport.services;

import com.unimag.bustransport.domain.entities.SeatHold;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Servicio para gestión de reservas temporales de asientos (SeatHolds)
 */
public interface SeatHoldService {

    /**
     * Crea una reserva temporal de asiento
     */
    SeatHold createSeatHold(Long tripId, String seatNumber, Long userId, OffsetDateTime expiresAt);

    /**
     * Libera una reserva temporal (marca como expirada)
    */
    void releaseSeatHold(Long holdId);

    /**
     * Busca reservas activas de un usuario en un viaje específico
     */
    List<SeatHold> getActiveHoldsByTripAndUser(Long tripId, Long userId);

    /**
     * Busca todas las reservas de un usuario (activas y expiradas)
     */
    List<SeatHold> getHoldsByUser(Long userId);

    /**
     * Verifica si un asiento está actualmente en hold (reservado temporalmente)
     */
    boolean isSeatOnHold(Long tripId, String seatNumber);

    boolean hasOverlappingHold(Long tripId, String seatNumber, Integer fromStopOrder, Integer toStopOrder);

    /**
     * Marca reservas como usadas (cuando se confirma la compra)
     */
    int markHoldsAsUsed(Long tripId, List<String> seatNumbers);

    /**
     * Expira automáticamente todas las reservas vencidas
     */
    int expireOldHolds();

    /**
     * Valida que las reservas existan, estén activas y no hayan expirado
     */
    void validateActiveHolds(Long tripId, List<String> seatNumbers, Long userId);
}
