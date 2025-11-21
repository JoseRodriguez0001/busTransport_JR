package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.SeatRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.BusService;
import com.unimag.bustransport.services.mapper.BusMapper;
import com.unimag.bustransport.services.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository repository;
    private final SeatRepository seatRepository;
    private final BusMapper mapper;
    private final SeatMapper seatMapper;

    private static final int COLUMS = 4;
    private static final char[] COLUMN_LETTERS = {'A','B','C','D'};
    @Override
    public BusDtos.BusResponse createBus(BusDtos.BusCreateRequest request) {
        repository.findByPlate(request.plate()).ifPresent(existingBus -> {
            log.warn("IAttempt to create a bus with a duplicate license plate: {}", request.plate());
            throw new DuplicateResourceException(
                    String.format("Bus already exists with plate '%s'", request.plate()));

        });

        if (request.capacity() % COLUMS != 0) {
            log.warn("capacity {} no es multiplo de {}",request.capacity(),COLUMS);
            throw new IllegalArgumentException(
                    String.format("capacity must be multiplo de %d", COLUMS)
            );
        }
        Bus bus = mapper.toEntity(request);

        Bus savedBus = repository.save(bus);
        log.info("Bus created succesfully with  ID: {} and plate: {}", savedBus.getId(), savedBus.getPlate());

        createSeatsForBus(savedBus);
        return mapper.toResponse(savedBus);
    }

    private void createSeatsForBus(Bus bus) {
        int rows = bus.getCapacity()/COLUMS;

        List<Seat> seats =  new ArrayList<>();
        for (int row=1;row<=rows;row++) {
            for (int col=0;col<COLUMS;col++) {
                String seatNumber= row + String.valueOf(COLUMN_LETTERS[col]);

                Seat.Type seatType = (row==1)? Seat.Type.PREFERENTIAL : Seat.Type.STANDARD;

                Seat seat = Seat.builder()
                        .number(seatNumber)
                        .type(seatType)
                        .bus(bus)
                        .build();

                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);
        log.info("Seats created succesfully");
    }

    @Override
    public void updateBus(Long id, BusDtos.BusUpdateRequest request) {
        Bus bus = repository.findById(id).orElseThrow(
                ()-> new NotFoundException("Bus not found")
        );

        if (request.capacity() % COLUMS != 0) {
            log.warn("capacity {} no es multiplo de {}",request.capacity(),COLUMS);
            throw new IllegalArgumentException(
                    String.format("capacity must be multiplo de %d", COLUMS)
            );
        }

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
        bus.setStatus(Bus.Status.RETIRED);
        repository.save(bus);
    }

    @Override
    public BusDtos.BusResponse getBus(Long id) {
        Bus bus = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Bus not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("Bus with ID %d not found", id)
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
                    log.error("Bus not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("Bus with ID %d not found", id)
                    );
                });

        List<SeatDtos.SeatResponse> seats = bus.getSeats().stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());

        return seats;
    }
}
