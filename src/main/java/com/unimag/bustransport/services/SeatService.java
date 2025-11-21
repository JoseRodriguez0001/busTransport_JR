package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Seat;
import java.util.List;

public interface SeatService {
    SeatDtos.SeatResponse createSeat(SeatDtos.SeatCreateRequest request);
    void updateSeat(Long id,SeatDtos.SeatUpdateRequest request);
    void deleteSeat(Long id);
    List<SeatDtos.SeatResponse> getSeatsByBusId(Long busId);
    List<SeatDtos.SeatResponse> getSeatsByBusIdAndType(Long busId, Seat.Type seatType);
    SeatDtos.SeatResponse getSeatById(Long id);
    boolean isSeatAvailable(Long tripId, String seatNumber, Long fromStopId, Long toStopId);
    void confirmSeatReservation(Long tripId, List<String> seatNumbers, Long purchaseId);
}
