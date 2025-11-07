package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.SeatHoldService;
import com.unimag.bustransport.services.SeatService;
import com.unimag.bustransport.services.TicketService;
import com.unimag.bustransport.services.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {
    private final SeatRepository repository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;
    private final StopRepository stopRepository;
    private final PurchaseRepository purchaseRepository;
    private final SeatHoldService seatHoldService;
    private final SeatMapper seatMapper;

    // Patrón para validar formato de número de asiento (ej: "1A", "12B", "5C")
    private static final Pattern SEAT_NUMBER_PATTERN = Pattern.compile("^[0-9]{1,3}[A-Z]$");

    @Override
    public SeatDtos.SeatResponse createSeat(SeatDtos.SeatCreateRequest request) {
        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(()-> new NotFoundException("Bus not found"));
        // Validar formato del número de asiento
        validateSeatNumber(request.number());

        //valida duplicados
        repository.findByBusIdAndNumber(request.busId(), request.number()).
                ifPresent(existingSeat -> {
                    throw new DuplicateResourceException("Seat already exists");
                });
        long currentSeatCount = repository.countByBusId(request.busId());
        if (currentSeatCount>= bus.getCapacity()){
            throw new IllegalArgumentException("Capacity bus exceeded");
        }

        Seat seat = seatMapper.toEntity(request);
        seat.setBus(bus);

        Seat seatSaved = repository.save(seat);
        return  buildSeatResponse(seatSaved,false);
    }

    @Override
    public void updateSeat(Long id, SeatDtos.SeatUpdateRequest request) {
        Seat seat = repository.findById(id)
                .orElseThrow(()-> new NotFoundException("Seat not found"));


        // Si se está cambiando el número, validar
        if (request.number() != null && !request.number().equals(seat.getNumber())) {
            // Validar formato
            validateSeatNumber(request.number());

            // Verificar que no haya tickets vendidos con el número actual
            boolean hasTickets = ticketRepository.existsByTripIdAndSeatNumberAndStatus(
                    seat.getBus().getId(), // Necesitamos buscar en cualquier trip del bus
                    seat.getNumber(),
                    Ticket.Status.SOLD
            );

            if (hasTickets) {
                throw new IllegalArgumentException(
                        String.format("Cannot change seat number. Seat %s has sold tickets",
                                seat.getNumber())
                );
            }

            // Validar que no exista duplicado con el nuevo número
            repository.findByBusIdAndNumber(seat.getBus().getId(), request.number())
                    .ifPresent(existingSeat -> {
                        throw new DuplicateResourceException(
                                String.format("Seat %s already exists in bus %s",
                                        request.number(), seat.getBus().getPlate())
                        );
                    });
        }

        seatMapper.updateEntityFromRequest(request, seat);

        repository.save(seat);
    }

    @Override
    public void deleteSeat(Long id) {
        Seat seat = repository.findById(id)
                .orElseThrow(()-> new NotFoundException("Seat not found"));

        List<Trip> trips = tripRepository.findByBusIdAndStatus(seat.getBus().getId(),Trip.Status.SCHEDULED).stream().toList();

        for (Trip trip : trips) {
            boolean hasTickets = ticketRepository.existsByTripIdAndSeatNumberAndStatus(
                    trip.getId(),
                    seat.getNumber(),
                    Ticket.Status.SOLD
            );

            if (hasTickets) {
                throw new IllegalArgumentException("Cannot delete seat that has sold tickets in trip " + trip.getId());
            }

            boolean hasActiveHolds = seatHoldService.isSeatOnHold(trip.getId(), seat.getNumber());
            if (hasActiveHolds) {
                throw new IllegalArgumentException("Cannot delete seat that has active holds in trip " + trip.getId());
            }
        }


        repository.delete(seat);
    }

    @Override
    public List<SeatDtos.SeatResponse> getSeatsByBusId(Long busId) {
        if (!busRepository.existsById(busId)) {
            throw new NotFoundException("Bus not found");
        }

        List<Seat> seats = repository.findByBusIdOrderByNumberAsc(busId);

        return seats.stream().map(seat -> buildSeatResponse(seat,false)).toList();
    }

    @Override
    public List<SeatDtos.SeatResponse> getSeatsByBusIdAndType(Long busId,Seat.Type seatType) {
        if (!busRepository.existsById(busId)) {
            throw new NotFoundException("Bus not found");
        }

        List<Seat> seats = repository.findByBusIdAndType(busId,seatType);

        return seats.stream().map(seat -> buildSeatResponse(seat,false)).toList();

    }

    @Override
    public SeatDtos.SeatResponse getSeatById(Long id) {
        Seat seat = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat with ID %d not found", id)
                ));

        return buildSeatResponse(seat, false);
    }

    @Override
    public boolean isSeatAvailable(Long tripId, String seatNumber, Long fromStopId, Long toStopId) {

        // Validar que el trip existe y obtener su bus
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        // Validar que los stops existen y obtener sus orders
        Stop fromStop = stopRepository.findById(fromStopId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Stop with ID %d not found", fromStopId)
                ));

        Stop toStop = stopRepository.findById(toStopId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Stop with ID %d not found", toStopId)
                ));

        // Validar que los stops pertenecen a la ruta del trip
        if (!fromStop.getRoute().getId().equals(trip.getRoute().getId()) ||
                !toStop.getRoute().getId().equals(trip.getRoute().getId())) {
            throw new IllegalArgumentException("Stops do not belong to the trip's route");
        }

        // Validar orden lógico
        if (fromStop.getOrder() >= toStop.getOrder()) {
            throw new IllegalArgumentException("fromStop order must be less than toStop order");
        }

        // Verificar que el asiento existe en el bus del trip
        boolean seatExists = repository.findByBusIdAndNumber(trip.getBus().getId(), seatNumber)
                .isPresent();

        if (!seatExists) {
            log.warn("Seat {} does not exist in bus {}", seatNumber, trip.getBus().getPlate());
            return false;
        }

        // Verificar si hay tickets vendidos que se solapen con el tramo
        List<Ticket> overlappingTickets = ticketRepository.findOverlappingTickets(
                tripId,
                seatNumber,
                fromStop.getOrder(),
                toStop.getOrder()
        );

        if (!overlappingTickets.isEmpty()) {
            log.info("Seat {} is not available. Found {} overlapping tickets",
                    seatNumber, overlappingTickets.size());
            return false;
        }

        // Verificar si hay holds activos que se solapen
        boolean hasHold = seatHoldService.hasOverlappingHold(
                tripId,
                seatNumber,
                fromStop.getOrder(),
                toStop.getOrder()
        );

        if (hasHold) {
            log.info("Seat {} is not available. It has an active hold", seatNumber);
            return false;
        }

        return true;
    }

    @Override
    public void confirmSeatReservation(Long tripId, List<String> seatNumbers, Long purchaseId) {

        // Validar que el trip existe
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        // Validar que la compra existe y está confirmada
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Purchase with ID %d not found", purchaseId)
                ));

        if (purchase.getPaymentStatus() != Purchase.PaymentStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    String.format("Purchase %d is not confirmed. Current status: %s",
                            purchaseId, purchase.getPaymentStatus())
            );
        }

        // Validar que todos los asientos existen en el bus del trip
        for (String seatNumber : seatNumbers) {
            boolean seatExists = repository.findByBusIdAndNumber(
                    trip.getBus().getId(), seatNumber
            ).isPresent();

            if (!seatExists) {
                throw new NotFoundException(
                        String.format("Seat %s does not exist in bus %s",
                                seatNumber, trip.getBus().getPlate())
                );
            }
        }

        // Validar que los SeatHolds existen, están activos y pertenecen al usuario de la compra
        seatHoldService.validateActiveHolds(tripId, seatNumbers, purchase.getUser().getId());

        // Verificar que los asientos no estén ya vendidos (doble verificación)
        for (String seatNumber : seatNumbers) {
            boolean alreadySold = ticketRepository.existsByTripIdAndSeatNumberAndStatus(
                    tripId, seatNumber, Ticket.Status.SOLD
            );

            if (alreadySold) {
                throw new IllegalArgumentException(
                        String.format("Seat %s is already sold for trip %d", seatNumber, tripId)
                );
            }
        }
        // Los tickets deberían crearse en el PurchaseService o mediante el TicketService
        // Aquí solo confirmamos que los holds se marcaron correctamente
    }

    private void validateSeatNumber(String number) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Seat number cannot be null or empty");
        }

        if (!SEAT_NUMBER_PATTERN.matcher(number.toUpperCase()).matches()) {
            throw new IllegalArgumentException(
                    String.format("Invalid seat number format: %s. Expected format: 1A, 12B, etc.",
                            number)
            );
        }
    }

    private SeatDtos.SeatResponse buildSeatResponse(Seat seat, boolean occupied) {
        return new SeatDtos.SeatResponse(
                seat.getId(),
                seat.getNumber(),
                seat.getType().toString(),
                seat.getBus().getId(),
                seat.getBus().getPlate(),
                occupied
        );
    }


}
