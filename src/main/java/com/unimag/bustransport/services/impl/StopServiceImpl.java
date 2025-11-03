package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.services.StopService;

import java.util.List;

public class StopServiceImpl implements StopService {
    @Override
    public StopDtos.StopResponse createStop(StopDtos.StopCreateRequest request) {
        return null;
    }

    @Override
    public void updateStop(Long id, StopDtos.StopUpdateRequest request) {

    }

    @Override
    public void deleteStop(Long id) {

    }

    @Override
    public List<StopDtos.StopResponse> getStopsByRouteId(Long id) {
        return List.of();
    }

    @Override
    public StopDtos.StopResponse getStopById(Long id) {
        return null;
    }

    @Override
    public List<StopDtos.StopResponse> getStopsBetween(Long from, Long to) {
        return List.of();
    }
}
