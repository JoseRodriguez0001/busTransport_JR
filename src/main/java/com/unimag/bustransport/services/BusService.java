package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;

import java.util.List;

public interface BusService {
    BusDtos.BusResponse createBus(BusDtos.BusCreateRequest request);
    void updateBus(Long id,BusDtos.BusUpdateRequest request);
    void deleteBus(Long id);
    BusDtos.BusResponse getBus(Long id);
    List<BusDtos.BusResponse> getAllBus();
    List<SeatDtos.SeatResponse> getAllSeatsByBusId(Long id);
}
