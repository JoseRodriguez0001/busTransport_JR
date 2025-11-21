package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.api.dto.TripDtos;

import java.time.LocalDate;
import java.util.List;

public interface TripService {
    TripDtos.TripResponse createTrip(TripDtos.TripCreateRequest request);
    void updateTrip(Long id,TripDtos.TripUpdateRequest request);
    void deleteTrip(Long id);
    List<TripDtos.TripResponse> getTrips(String origin, String destination, LocalDate date);
    TripDtos.TripResponse getTripDetails(Long tripId);
    List<SeatDtos.SeatResponse> getSeats(Long tripId);
    Long getTripStatistics(Long tripId);
}
