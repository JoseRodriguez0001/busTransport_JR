package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.InvalidCredentialsException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.TicketService;
import com.unimag.bustransport.services.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final PassengerRepository passengerRepository;
    private final StopRepository stopRepository;
    private final PurchaseRepository purchaseRepository;
    private final TicketMapper ticketMapper;

    private static final int TICKET_CLEANUP_MINUTES = 15;

    @Override
    public TicketDtos.TicketResponse createTicket(TicketDtos.TicketCreateRequest request) {

        // 1. Validar existencia de entidades relacionadas
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> {
                    log.error("Trip not found with ID: {}", request.tripId());
                    return new NotFoundException(
                            String.format("Trip with ID %d not found", request.tripId())
                    );
                });

        Passenger passenger = passengerRepository.findById(request.passengerId())
                .orElseThrow(() -> {
                    log.error("Passenger not found with ID: {}", request.passengerId());
                    return new NotFoundException(
                            String.format("Passenger with ID %d not found", request.passengerId())
                    );
                });

        Stop fromStop = stopRepository.findById(request.fromStopId())
                .orElseThrow(() -> {
                    log.error("Origin stop not found with ID: {}", request.fromStopId());
                    return new NotFoundException(
                            String.format("Stop with ID %d not found", request.fromStopId())
                    );
                });

        Stop toStop = stopRepository.findById(request.toStopId())
                .orElseThrow(() -> {
                    log.error("Destination stop not found with ID: {}", request.toStopId());
                    return new NotFoundException(
                            String.format("Stop with ID %d not found", request.toStopId())
                    );
                });

        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> {
                    log.error("Purchase not found with ID: {}", request.purchaseId());
                    return new NotFoundException(
                            String.format("Purchase with ID %d not found", request.purchaseId())
                    );
                });

        // 2. Validar que las paradas pertenezcan a la ruta del trip
        if (!fromStop.getRoute().getId().equals(trip.getRoute().getId())) {
            log.error("The origin stop does not belong to the trip route");
            throw new IllegalArgumentException(
                    "The origin stop does not belong to the trip route"
            );
        }

        if (!toStop.getRoute().getId().equals(trip.getRoute().getId())) {
            log.error("The destination stop does not belong to the trip route");
            throw new IllegalArgumentException(
                    "The destination stop does not belong to the trip route"
            );
        }

        // 3. Validar que fromStop.order < toStop.order
        if (fromStop.getOrder() >= toStop.getOrder()) {
            log.error("The order of the origin stop ({}) must be less than the destination ({})",
                    fromStop.getOrder(), toStop.getOrder());
            throw new IllegalArgumentException(
                    "The origin stop must be before the destination stop on the route"
            );
        }

        // 4. Verificar disponibilidad del asiento en el tramo (solo SOLD)
        List<Ticket> overlappingTickets = ticketRepository.findOverlappingTickets(
                request.tripId(),
                request.seatNumber(),
                fromStop.getOrder(),
                toStop.getOrder()
        );

        if (!overlappingTickets.isEmpty()) {
            log.warn("Asiento {} ya está ocupado en el tramo solicitado del trip {}",
                    request.seatNumber(), request.tripId());
            throw new DuplicateResourceException(
                    String.format("El asiento %s ya está ocupado en el tramo seleccionado",
                            request.seatNumber())
            );
        }

        // 5. Crear entidad y establecer relaciones
        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setTrip(trip);
        ticket.setPassenger(passenger);
        ticket.setFromStop(fromStop);
        ticket.setToStop(toStop);
        ticket.setPurchase(purchase);
        ticket.setStatus(Ticket.Status.PENDING);  // Crear en PENDING

        // 6. NO generar QR todavía (se genera al confirmar purchase)

        // 7. Guardar ticket
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully with ID: {} in PENDING status",
                savedTicket.getId());

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public void deleteTicket(Long id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ticket no encontrado con ID: {}", id);
                    return new NotFoundException(
                            String.format("Ticket con ID %d no encontrado", id)
                    );
                });

        // Cambiar status a CANCELLED
        ticket.setStatus(Ticket.Status.CANCELLED);
        ticketRepository.save(ticket);

        log.info("Ticket ID: {} cancelado exitosamente", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDtos.TicketResponse getTicket(Long id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> {
                    return new NotFoundException(
                            String.format("Ticket con ID %d no encontrado", id)
                    );
                });

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByTrip(Long tripId) {

        List<Ticket> tickets = ticketRepository.findByTripId(tripId);

        log.info("Se encontraron {} tickets para el trip ID: {}", tickets.size(), tripId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByPurchase(Long purchaseId) {

        List<Ticket> tickets = ticketRepository.findByPurchaseId(purchaseId);

        log.info("Se encontraron {} tickets para la compra ID: {}", tickets.size(), purchaseId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByPassenger(Long passengerId) {

        List<Ticket> tickets = ticketRepository.findByPassengerId(passengerId);

        log.info("Se encontraron {} tickets para el pasajero ID: {}", tickets.size(), passengerId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void generateQrForTicket(Long ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    return new NotFoundException(
                            String.format("Ticket con ID %d no encontrado", ticketId)
                    );
                });

        // Generar nuevo QR
        String newQrCode = generateUniqueQrCode();
        ticket.setQrCode(newQrCode);
        ticketRepository.save(ticket);

        log.info("QR generado exitosamente para ticket ID: {} - QR: {}", ticketId, newQrCode);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateQrForTicket(String qrCode) {

        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> {
                    return new NotFoundException(
                            String.format("Ticket con QR '%s' no encontrado", qrCode)
                    );
                });

        // Validar que el status sea SOLD
        if (ticket.getStatus() != Ticket.Status.SOLD) {
            throw new InvalidCredentialsException(
                    String.format("El ticket no está activo (status: %s)", ticket.getStatus())
            );
        }

        log.info("QR validated successfully for ticket ID: {}", ticket.getId());
    }

// Limpiar tickets PENDING antiguos
    @Override
    @Scheduled(cron = "0 */5 * * * *")  // Cada 5 minutos
    public int expireOldTickets() {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(TICKET_CLEANUP_MINUTES);

        List<Ticket> expiredTickets = ticketRepository.findExpiredPendingTickets(cutoffTime);

        if (expiredTickets.isEmpty()) {
            return 0;
        }

        ticketRepository.deleteAll(expiredTickets);

        log.info("Cleaned up {} expired PENDING tickets (older than {} minutes)",
                expiredTickets.size(), TICKET_CLEANUP_MINUTES);

        return expiredTickets.size();
    }

    private String generateUniqueQrCode() {
        return "TICKET-" + UUID.randomUUID().toString().toUpperCase();
    }
}