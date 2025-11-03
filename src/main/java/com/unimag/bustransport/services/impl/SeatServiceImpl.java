package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.services.SeatService;

import java.util.List;

public class SeatServiceImpl implements SeatService {
    @Override
    public SeatDtos.SeatResponse createSeat(SeatDtos.SeatCreateRequest request) {
        return null;
    }

    @Override
    public void updateSeat(Long id, SeatDtos.SeatUpdateRequest request) {

    }

    @Override
    public void deleteSeat(Long id) {

    }

    @Override
    public List<SeatDtos.SeatResponse> getSeatsByUserId(Long id) {
        return List.of();
    }

    @Override
    public List<SeatDtos.SeatResponse> getSeatsByType(Seat.Type seatType) {
        return List.of();
    }

    @Override
    public boolean isSeatAvailable(Long tripId, String seatNumber, Long from, Long to) {
        return false;
    }

    @Override
    public void confirmSeatReservation(Long tripId, List<String> seatNumbers, Long purchaseId) {

    }
}
