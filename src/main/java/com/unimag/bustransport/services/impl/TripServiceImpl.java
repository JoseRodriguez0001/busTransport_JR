package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.TripService;
import com.unimag.bustransport.services.mapper.TripMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.FormatterClosedException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
    private final TripRepository repository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripMapper mapper;
    @Override
    public TripDtos.TripResponse createTrip(TripDtos.TripCreateRequest request) {
        //valido que route y bus existan
        // Validar que la ruta existe
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Route with ID %d not found", request.routeId())
                ));

        // Validar que el bus existe y está activo
        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Bus with ID %d not found", request.busId())
                ));

        if (bus.getStatus()  != Bus.Status.ACTIVE) throw  new IllegalArgumentException(String.format("Bus with ID %d is not ACTIVE", request.busId()));

        //valido que las fechas sean oherentes
        validateTripDates(request.date(), request.departureAt(), request.arrivalAt());
        //validar que se cree un trip con buena fecha
        if (request.date().isBefore(LocalDate.now())) {
            throw  new IllegalArgumentException("Cannot create tripd in the past");
        }
        //validar overbooking porcentaje
        validateOverBookingPercent(request.overbookingPercent());
        //valido que el bus esté disponible
        validateBusAvailability(request.busId(),request.departureAt(),request.arrivalAt(),null);

        Trip trip = mapper.toEntity(request);
        trip.setRoute(route);
        trip.setBus(bus);
        trip.setStatus(Trip.Status.SCHEDULED);


        Trip tripSaved = repository.save(trip);

        log.info("Trip saved with ID {}", tripSaved.getId());

        return  buildTripResponse(tripSaved);
    }



    @Override
    public void updateTrip(Long id, TripDtos.TripUpdateRequest request) {
        Trip trip = repository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Trip with ID %d not found", id)));

        Long soldTickets = repository.countSoldTickets(id);

        if (soldTickets>0 && (request.departureAt() != null || request.arrivalAt() != null)) {
            throw  new IllegalArgumentException(String.format("Cannot modify dates. Trip has %d sold tickets", soldTickets));
        }

        //validar coherencia de fechas
        if (request.departureAt() != null ||  request.arrivalAt() != null) {
            OffsetDateTime newdepartureAt = request.departureAt() != null ? request.departureAt() : trip.getDepartureAt();
            OffsetDateTime newArrivalAt = request.arrivalAt() != null ? request.arrivalAt() : trip.getArrivalAt();

            validateTripDates(trip.getDate(), newdepartureAt, newArrivalAt);

            //validar disponibilidad del bus con nuevas fechas
            validateBusAvailability(trip.getBus().getId(),newdepartureAt,newArrivalAt,id);
        }
        //valido cambios de estados
        if (request.status() != null && !request.status().equals(trip.getStatus())) {
            validateStatusTransition(trip.getStatus(),request.status(),soldTickets);

        }

        //validar overbooking
        if (request.overbookingPercent() != null){
            validateOverBookingPercent(request.overbookingPercent());
        }

        mapper.updateEntityFromRequest(request, trip);
        repository.save(trip);
        log.info("Trip updated with ID {}", trip.getId());
    }

    @Override
    public void deleteTrip(Long id) {
        Trip trip = repository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Trip with ID %d not found", id)));

        long soldTickets = repository.countSoldTickets(id);
        if (soldTickets>0){
            throw  new IllegalArgumentException(String.format("Cannot delete trip. It has %d sold tickets", soldTickets));
        }

        repository.delete(trip);
        log.info("Trip deleted with ID {}", id);
    }

    @Override
    public List<TripDtos.TripResponse> getTrips(String origin, String destination, LocalDate date) {

        // Validar parámetros
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin cannot be null or empty");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination cannot be null or empty");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        List<Trip> trips = repository.findTripsByOriginAndDestination(origin, destination, date);

        log.info("Found {} trips in origin and destination {}", trips.size(), origin);

        return trips.stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public TripDtos.TripResponse getTripDetails(Long tripId) {

        Trip trip = repository.findByIdWithBusAndSeats(tripId).
                orElseThrow(()-> new NotFoundException(String.format("Trip with %d not found", tripId)));
        log.info("Trip statistics for trip with ID {}", tripId);
        return buildTripResponse(trip);
    }

    @Override
    public List<SeatDtos.SeatResponse> getSeats(Long tripId) {

        Trip trip = repository.findByIdWithBusAndSeats(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        Bus bus = trip.getBus();
        List<Seat> seats = bus.getSeats();

        if (seats == null || seats.isEmpty()) {
            log.warn("Bus {} has no seats configured",bus.getPlate());
            return new ArrayList<>();
        }

        //obtener asientos ocupados
        List<String> ocuppiedSeatNumbers = trip.getTickets().stream()
                .map(Ticket::getSeatNumber).toList();

        //obtener asientos en hold
        OffsetDateTime now = OffsetDateTime.now();
        List<String> seatsInHold = trip.getSeatHolds().stream()
                .filter(hold -> hold.getStatus() == SeatHold.Status.HOLD)
                .filter(hold -> hold.getExpiresAt().isAfter(now))
                .map(SeatHold::getSeatNumber)
                .toList();

        List<SeatDtos.SeatResponse> seatsResponse = seats.stream()
                .map(seat -> {
                    boolean isOcuppied = ocuppiedSeatNumbers.contains(seat.getNumber())
                            || seatsInHold.contains(seat.getNumber());

                    return new SeatDtos.SeatResponse(
                            seat.getId(),
                            seat.getNumber(),
                            seat.getType().toString(),
                            bus.getId(),
                            bus.getPlate(),
                            isOcuppied
                    );
                }).toList();
        return seatsResponse;
    }

    @Override
    public Long getTripStatistics(Long tripId) {
        if (!repository.existsById(tripId)) {
            throw new NotFoundException(
                    String.format("Trip with ID %d not found", tripId)
            );
        }

        // Retornar conteo de tickets vendidos
        Long soldTickets = repository.countSoldTickets(tripId);

        log.info("Trip ID: {} has {} sold tickets", tripId, soldTickets);
        return soldTickets;
    }


    private void validateTripDates(@NotNull LocalDate date, @NotNull OffsetDateTime departureAt, @NotNull OffsetDateTime arrivalAt) {
        if (!departureAt.isBefore(arrivalAt)) {
            throw new IllegalArgumentException("Departure must be before Arrival");
        }
        /*
        if (!date.equals(departureAt.toLocalDate())) {
            throw new IllegalArgumentException("Departure must be after Arrival");
        }*/

        long durationHours = ChronoUnit.HOURS.between(departureAt, arrivalAt);
        if (durationHours>32){
            throw new IllegalArgumentException("Departure time must be less than 32 hours");
        }
    }

    private void validateOverBookingPercent(Double overbookingPercent) {
        if (overbookingPercent != null) {
            if (overbookingPercent < 0 || overbookingPercent > 15) {
                throw new IllegalArgumentException("Overbooking percent must be between 0 and 15");
            }
        }
    }

    private void validateBusAvailability(Long busId, OffsetDateTime departureAt, OffsetDateTime arrivalAt, Long excludeTripId){
        //viajes activos del bus en esos estados
        List<Trip> activeTrips = new ArrayList<>(repository.findByBusIdAndStatus(busId, Trip.Status.SCHEDULED));
        activeTrips.addAll(repository.findByBusIdAndStatus(busId, Trip.Status.BOARDING));
        activeTrips.addAll(repository.findByBusIdAndStatus(busId, Trip.Status.DEPARTED));

        //filtramos el trip actual
        if (excludeTripId != null) {
            activeTrips = activeTrips.stream()
                    .filter(trip -> !trip.getId().equals(excludeTripId))
                    .collect(Collectors.toList());
        }
        //verificar solapamiento de horarios
        for (Trip existingTrip : activeTrips) {
            boolean hasOverlap = checkTimeOverlap(departureAt,arrivalAt
                    ,existingTrip.getDepartureAt(), existingTrip.getArrivalAt());
            if (hasOverlap) {
                throw new DuplicateResourceException(String.format("Bus is not available. It has Another trip (ID : %d) from %s to %s", existingTrip
                        .getId(), existingTrip.getDepartureAt(), existingTrip.getArrivalAt()));

            }
        }

    }



    private boolean checkTimeOverlap(OffsetDateTime start1, OffsetDateTime end1,
                                     OffsetDateTime start2, OffsetDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private void validateStatusTransition(Trip.Status cStatus, Trip.Status newStatus, Long soldTickets) {
        //no permitir cancelar si ya llegó o está en camino sin tickets vendidos
        if (newStatus == Trip.Status.CANCELLED) {
            if (cStatus == Trip.Status.ARRIVED) {
                throw new IllegalArgumentException("Cannot cancel a trip that has already arrived");
            }
            if (cStatus == Trip.Status.DEPARTED && soldTickets > 0) {
                throw new IllegalArgumentException("Cannot cancel a trip that has already departed with Passengers");
            }
        }

        // Validar flujo lógico de estados
        switch (cStatus) {
            case SCHEDULED:
                if (newStatus != Trip.Status.BOARDING && newStatus != Trip.Status.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Scheduled trip can only transition to BOARDING or CANCELLED"
                    );
                }
                break;
            case BOARDING:
                if (newStatus != Trip.Status.DEPARTED && newStatus != Trip.Status.CANCELLED) {
                    throw new IllegalArgumentException(
                            "Boarding trip can only transition to DEPARTED or CANCELLED"
                    );
                }
                break;
            case DEPARTED:
                if (newStatus != Trip.Status.ARRIVED) {
                    throw new IllegalArgumentException(
                            "Departed trip can only transition to ARRIVED"
                    );
                }
                break;
            case ARRIVED:
                throw new IllegalArgumentException("Cannot change status of an arrived trip");
            case CANCELLED:
                throw new IllegalArgumentException("Cannot change status of a cancelled trip");
        }
    }

    private TripDtos.TripResponse buildTripResponse(Trip trip) {
        TripDtos.TripResponse response = mapper.toResponse(trip);
        Long soldSeats = repository.countSoldTickets(trip.getId());
        // asirntos disponibles
        Integer capacity = trip.getBus().getCapacity();
        Double overBooking = trip.getOverbookingPercent() != null ? trip.getOverbookingPercent() : 0.0;

        int maxCapacoty = capacity+ (int) Math.floor(capacity * overBooking / 100);
        int availableSeats = Math.max(0, maxCapacoty - soldSeats.intValue());

        //dto con t-odo calculado
        return new TripDtos.TripResponse(
                response.id(),
                response.route(),
                response.bus(),
                response.date(),
                response.departureAt(),
                response.arrivalAt(),
                response.durationMinutes(),
                response.overbookingPercent(),
                response.status(),
                soldSeats.intValue(),
                availableSeats
        );
    }
}
