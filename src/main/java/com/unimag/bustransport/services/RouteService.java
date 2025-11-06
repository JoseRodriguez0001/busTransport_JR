package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.RouteDtos;
import com.unimag.bustransport.api.dto.StopDtos;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteService {
    RouteDtos.RouteResponse createRoute(RouteDtos.RouteCreateRequest request);
    void updateRoute(Long id,RouteDtos.RouteUpdateRequest request);
    void deleteRoute(Long id);
    List<RouteDtos.RouteResponse> getAllRoutes();
    RouteDtos.RouteResponse getRouteById(Long id);
    List<StopDtos.StopResponse>  getStopsByRouteId(Long id);
    List<RouteDtos.RouteResponse> searchRoutes(String origin, String destination);
}
