package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.entities.SeatHold;

import java.time.LocalDate;
import java.util.List;

public interface SeatService {
    SeatDtos.SeatResponse createSeat(SeatDtos.SeatCreateRequest request);
    void updateSeat(Long id,SeatDtos.SeatUpdateRequest request);
    void deleteSeat(Long id);
    List<SeatDtos.SeatResponse> getSeatsByUserId(Long id);
    List<SeatDtos.SeatResponse> getSeatsByType(Seat.Type seatType);
    boolean isSeatAvailable(Long tripId, String seatNumber, Long from, Long to);
    void confirmSeatReservation(Long tripId, List<String> seatNumbers, Long purchaseId); // pendiente verificar en seathold
}
