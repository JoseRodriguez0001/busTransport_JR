package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.SeatHoldDtos;
import com.unimag.bustransport.domain.entities.SeatHold;

import java.time.OffsetDateTime;
import java.util.List;

public interface SeatHoldService {

    SeatHoldDtos.SeatHoldResponse createSeatHold(SeatHoldDtos.SeatHoldCreateRequest request);

    void releaseSeatHold(Long holdId);

    SeatHoldDtos.SeatHoldResponse getHoldById(Long holdId);

    List<SeatHoldDtos.SeatHoldResponse> getActiveHoldsByTripAndUser(Long tripId, Long userId);

    List<SeatHoldDtos.SeatHoldResponse> getActiveHoldsByTrip(Long tripId);

    List<SeatHoldDtos.SeatHoldResponse> getHoldsByUser(Long userId);

    boolean isSeatOnHold(Long tripId, String seatNumber);

    //aca los marco como expirados cada x tienpo(por si no alcanza a crear la compra y se la pasa el tiempo)
    int markExpiredHolds();

    //aca borro de la db los que ya expiraron
    int deleteExpiredHolds();

    void validateActiveHolds(Long tripId, List<String> seatNumbers, Long userId);

    OffsetDateTime calculateExpirationTime();

    boolean hasOverlappingHold(Long tripId, String seatNumber, Integer fromStopOrder, Integer toStopOrder);

}