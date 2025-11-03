package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.services.TripService;

import java.time.LocalDate;
import java.util.List;

public class TripServiceImpl implements TripService {
    @Override
    public TripDtos.TripResponse createTrip(TripDtos.TripCreateRequest request) {
        return null;
    }

    @Override
    public void updateTrip(Long id, TripDtos.TripUpdateRequest request) {

    }

    @Override
    public void deleteTrip(Long id) {

    }

    @Override
    public List<TripDtos.TripResponse> getTrips(String origin, String destination, LocalDate date) {
        return List.of();
    }

    @Override
    public List<TripDtos.TripResponse> getTripDetails(Long tripId) {
        return List.of();
    }

    @Override
    public SeatDtos.SeatResponse getSeats(Long tripId) {
        return null;
    }

    @Override
    public Long getTripStatistics(Long tripId) {
        return 0L;
    }
}
