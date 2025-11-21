package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.services.StopService;
import com.unimag.bustransport.services.mapper.StopMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StopServiceImpl implements StopService {

    private final StopRepository repository;
    private final RouteRepository  routeRepository;
    private final StopMapper mapper;

    @Override
    public StopDtos.StopResponse createStop(StopDtos.StopCreateRequest request) {
        Route route = routeRepository.findById(request.routeId()).
                orElseThrow(() -> new RuntimeException("Route not found"));
        if (request.order()<0) throw new IllegalArgumentException("Order cannot be less than 0");

        repository.findByRouteIdAndOrder(route.getId(), request.order()).
                ifPresent(stop -> {
                    throw new DuplicateResourceException(String.format("Stop with id %d already exists", stop.getId()));
                });
        Stop stop = mapper.toEntity(request);
        stop.setRoute(route);
        Stop stopSaved = repository.save(stop);
        log.info("Created stop with id {}", stopSaved.getId());
        return mapper.toResponse(stopSaved);
    }

    @Override
    public void updateStop(Long id, StopDtos.StopUpdateRequest request) {

        Stop stop = repository.findById(id).orElseThrow(() -> new RuntimeException("Stop not found"));

        if (request.order() != null && !request.order().equals(stop.getOrder())){

            if (request.order()<0) throw new IllegalArgumentException("Order cannot be less than 0");

            repository.findByRouteIdAndOrder(stop.getRoute().getId(), request.order()).ifPresent(stop1 ->{
                throw new DuplicateResourceException(String.format("Stop with id %d already exists", stop1.getId()));
            } );
        }

        mapper.updateEntityFromRequest(request, stop);
        repository.save(stop);
        log.info("Updated stop with id {}", stop.getId());
    }

    @Override
    public void deleteStop(Long id) {
        Stop stop = repository.findById(id).orElseThrow(() -> new RuntimeException("Stop not found"));

        int fareRulesSize = stop.getFareRulesFrom().size() + stop.getFareRulesTo().size();
        if (fareRulesSize > 0){
            log.warn("Deleting FareRules Associated with Stop with id {}", stop.getId());
        }
        repository.delete(stop);
        log.info("Deleted stop with id {}", stop.getId());
    }

    @Override
    public List<StopDtos.StopResponse> getStopsByRouteId(Long id) {
        if(!routeRepository.existsById(id)) throw new RuntimeException("Route not found");

        List<Stop> stops = repository.findByRouteIdOrderByOrderAsc(id);
        log.info("Found {} stops associated with route with id {}", stops.size(), id);
        return stops.stream().map(mapper::toResponse).toList();
    }

    @Override
    public StopDtos.StopResponse getStopById(Long id) {
        Stop stop = repository.findById(id).orElseThrow(() -> new RuntimeException("Stop not found"));
        return mapper.toResponse(stop);
    }

    @Override
    public List<StopDtos.StopResponse> getStopsBetween(Long routeId,Integer fromOrder, Integer toOrder) {
        if (!routeRepository.existsById(routeId)) throw new RuntimeException("Route not found");
        if (fromOrder<0 || toOrder<0) throw new IllegalArgumentException("Order cannot be less than 0");
        if (fromOrder>toOrder) throw new IllegalArgumentException("Order cannot be greater than or equal to order");
        List<Stop> stops = repository.findStopsBetween(routeId, fromOrder, toOrder);

        log.info("Found {} stops associated with route with id {}", stops.size(), routeId);

        return stops.stream().map(mapper::toResponse).toList();
    }
}
