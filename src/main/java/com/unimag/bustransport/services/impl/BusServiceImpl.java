package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.services.BusService;

import java.util.List;

public class BusServiceImpl implements BusService {
    @Override
    public BusDtos.BusResponse createBus(BusDtos.BusCreateRequest request) {
        return null;
    }

    @Override
    public void updateBus(Long id, BusDtos.BusUpdateRequest request) {

    }

    @Override
    public void deleteBus(Long id) {

    }

    @Override
    public BusDtos.BusResponse getBus(Long id) {
        return null;
    }

    @Override
    public List<BusDtos.BusResponse> getAllBus() {
        return List.of();
    }

    @Override
    public List<SeatDtos.SeatResponse> getAllSeatsByBusId(Long id) {
        return List.of();
    }
}
