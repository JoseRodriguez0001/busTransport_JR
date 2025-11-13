package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.RouteDtos;
import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.RouteService;
import com.unimag.bustransport.services.mapper.RouteMapper;
import com.unimag.bustransport.services.mapper.StopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteMapper routeMapper;
    private final StopMapper stopMapper;

    @Override
    public RouteDtos.RouteResponse createRoute(RouteDtos.RouteCreateRequest request) {
        validateUniqueCode(request.code());

        Route route = routeMapper.toEntity(request);
        routeRepository.save(route);

        return routeMapper.toResponse(route);
    }

    @Override
    public void updateRoute(Long id, RouteDtos.RouteUpdateRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ruta no encontrada"));

        if (request.name() != null) {
            route.setName(request.name());
        }

        if (request.origin() != null) {
            route.setOrigin(request.origin());
        }

        if (request.destination() != null) {
            route.setDestination(request.destination());
        }

        if (request.distanceKm() != null) {
            route.setDistanceKm(request.distanceKm());
        }

        if (request.durationMin() != null) {
            route.setDurationMin(request.durationMin());
        }

        routeRepository.save(route);
    }

    @Override
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ruta no encontrada"));

        routeRepository.delete(route);
    }

    @Override
    public List<RouteDtos.RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream().map(routeMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public RouteDtos.RouteResponse getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ruta no encontrada"));

        return routeMapper.toResponse(route);
    }

    @Override
    public List<StopDtos.StopResponse> getStopsByRouteId(Long id) {
        List<Stop> stops = stopRepository.findByRouteIdOrderByOrderAsc(id);

        if (stops.isEmpty()) {
            if (!routeRepository.existsById(id)) {
                throw new NotFoundException("Ruta no encontrada");
            }
        }

        return stops.stream().map(stopMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RouteDtos.RouteResponse> searchRoutes(String origin, String destination) {
        List<Route> routes;
        if (origin != null && destination != null) {
            routes = routeRepository.findByOriginAndDestination(origin, destination);
        } else if (origin != null) {
            routes = routeRepository.findByOrigin(origin);
        } else if (destination != null) {
            routes = routeRepository.findByDestination(destination);
        } else {
            throw new NotFoundException("No hay rutas disponibles");
        }

        return routes.stream().map(routeMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateUniqueCode(String code) {
        if (routeRepository.existsByCode(code)) {
            throw new IllegalStateException("Ya existe una ruta con el código: " + code);
        }
    }
}
