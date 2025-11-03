package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.StopDtos;

import java.util.List;

public interface StopService {
    StopDtos.StopResponse createStop(StopDtos.StopCreateRequest request);
    void updateStop(Long id,StopDtos.StopUpdateRequest request);
    void deleteStop(Long id);
    List<StopDtos.StopResponse> getStopsByRouteId(Long id);
    StopDtos.StopResponse getStopById(Long id);
    List<StopDtos.StopResponse> getStopsBetween(Long from, Long to);
}
