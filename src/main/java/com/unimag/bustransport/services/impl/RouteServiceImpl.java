package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.RouteDtos;
import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.services.RouteService;

import java.util.List;

public class RouteServiceImpl implements RouteService {

    @Override
    public RouteDtos.RouteResponse createRoute(RouteDtos.RouteCreateRequest request) {
        return null;
    }

    @Override
    public void updateRoute(Long id, RouteDtos.RouteUpdateRequest request) {

    }

    @Override
    public void deleteRoute(Long id) {

    }

    @Override
    public List<RouteDtos.RouteResponse> getAllRoutes() {
        return List.of();
    }

    @Override
    public RouteDtos.RouteResponse getRouteById(Long id) {
        return null;
    }

    @Override
    public List<StopDtos.StopResponse> getStopsByRouteId(Long id) {
        return List.of();
    }

    @Override
    public List<RouteDtos.RouteResponse> searchRoutes(String origin, String destination) {
        return List.of();
    }
}
