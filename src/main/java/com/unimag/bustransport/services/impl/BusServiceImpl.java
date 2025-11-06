package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.BusService;
import com.unimag.bustransport.services.mapper.BusMapper;
import com.unimag.bustransport.services.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository repository;
    private final BusMapper mapper;
    private final SeatMapper seatMapper;
    @Override
    public BusDtos.BusResponse createBus(BusDtos.BusCreateRequest request) {
        // Validar que la placa sea única
        repository.findByPlate(request.plate()).ifPresent(existingBus -> {
            log.warn("Intento de crear bus con placa duplicada: {}", request.plate());
            throw new DuplicateResourceException(
                    String.format("Ya existe un bus con la placa '%s'", request.plate()));

        });


        Bus bus = mapper.toEntity(request);

        // Guardar bus
        Bus savedBus = repository.save(bus);
        log.info("Bus creado exitosamente con ID: {} y placa: {}", savedBus.getId(), savedBus.getPlate());

        return mapper.toResponse(savedBus);
    }

    @Override
    public void updateBus(Long id, BusDtos.BusUpdateRequest request) {
        Bus bus = repository.findById(id).orElseThrow(
                ()-> new NotFoundException("Bus not found")
        );

        mapper.updateEntityFromRequest(request,bus);
        repository.save(bus);
    }

    @Override
    public void deleteBus(Long id) {
        Bus bus = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bus not found"));

        boolean hasActiveTrip = bus.getTrips().stream()
                .anyMatch(trip ->
                                trip.getStatus() == Trip.Status.SCHEDULED ||
                                trip.getStatus() == Trip.Status.BOARDING ||
                                trip.getStatus() == Trip.Status.DEPARTED

                );

        if (hasActiveTrip) {
            throw new IllegalArgumentException("Cannot delete bus: it is currently assigned to an active trip");
        }
        //cambiamos estado
        bus.setStatus(Bus.Status.RETIRED);
        repository.save(bus);
    }

    @Override
    public BusDtos.BusResponse getBus(Long id) {
        Bus bus = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Bus no encontrado con ID: {}", id);
                    return new NotFoundException(
                            String.format("Bus con ID %d no encontrado", id)
                    );
                });

        return mapper.toResponse(bus);
    }

    @Override
    public List<BusDtos.BusResponse> getAllBus() {
        List<Bus> buses = repository.findAll();

        return buses.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatDtos.SeatResponse> getAllSeatsByBusId(Long id) {
        Bus bus = repository.findByIdWithSeats(id)
                .orElseThrow(() -> {
                    log.error("Bus no encontrado con ID: {}", id);
                    return new NotFoundException(
                            String.format("Bus con ID %d no encontrado", id)
                    );
                });

        List<SeatDtos.SeatResponse> seats = bus.getSeats().stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());

        return seats;
    }
}
