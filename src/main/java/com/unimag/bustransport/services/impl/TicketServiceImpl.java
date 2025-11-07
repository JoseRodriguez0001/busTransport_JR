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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
  Este servicio es usado principalmente por PurchaseService.

 */
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

    /**
     Crea un nuevo ticket en el sistema.
      Valida todas las reglas de negocio críticas antes de crear el ticket.

      VALIDACIONES:
      - Trip, Passenger, Stops y Purchase deben existir
      - fromStop y toStop deben pertenecer a la ruta del Trip
      - fromStop.order debe ser menor que toStop.order
      - El asiento NO debe estar ocupado en ningún tramo solapado
     */
    @Override
    public TicketDtos.TicketResponse createTicket(TicketDtos.TicketCreateRequest request) {

        // 1. Validar existencia de entidades relacionadas
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> {
                    log.error("Trip no encontrado con ID: {}", request.tripId());
                    return new NotFoundException(
                            String.format("Trip con ID %d no encontrado", request.tripId())
                    );
                });

        Passenger passenger = passengerRepository.findById(request.passengerId())
                .orElseThrow(() -> {
                    log.error("Pasajero no encontrado con ID: {}", request.passengerId());
                    return new NotFoundException(
                            String.format("Pasajero con ID %d no encontrado", request.passengerId())
                    );
                });

        Stop fromStop = stopRepository.findById(request.fromStopId())
                .orElseThrow(() -> {
                    log.error("Parada de origen no encontrada con ID: {}", request.fromStopId());
                    return new NotFoundException(
                            String.format("Parada con ID %d no encontrada", request.fromStopId())
                    );
                });

        Stop toStop = stopRepository.findById(request.toStopId())
                .orElseThrow(() -> {
                    log.error("Parada de destino no encontrada con ID: {}", request.toStopId());
                    return new NotFoundException(
                            String.format("Parada con ID %d no encontrada", request.toStopId())
                    );
                });

        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> {
                    log.error("Compra no encontrada con ID: {}", request.purchaseId());
                    return new NotFoundException(
                            String.format("Compra con ID %d no encontrada", request.purchaseId())
                    );
                });

        // 2. Validar que las paradas pertenezcan a la ruta del trip
        if (!fromStop.getRoute().getId().equals(trip.getRoute().getId())) {
            log.error("La parada de origen no pertenece a la ruta del trip");
            throw new IllegalArgumentException(
                    "La parada de origen no pertenece a la ruta del viaje"
            );
        }

        if (!toStop.getRoute().getId().equals(trip.getRoute().getId())) {
            log.error("La parada de destino no pertenece a la ruta del trip");
            throw new IllegalArgumentException(
                    "La parada de destino no pertenece a la ruta del viaje"
            );
        }

        // 3. Validar que fromStop.order < toStop.order
        if (fromStop.getOrder() >= toStop.getOrder()) {
            log.error("El orden de la parada de origen ({}) debe ser menor que el destino ({})",
                    fromStop.getOrder(), toStop.getOrder());
            throw new IllegalArgumentException(
                    "La parada de origen debe estar antes que la parada de destino en la ruta"
            );
        }

        // Verificar disponibilidad del asiento en el tramo
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
        ticket.setStatus(Ticket.Status.SOLD);

        // 6. Generar QR único
        ticket.setQrCode(generateUniqueQrCode());

        // 7. Guardar ticket
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket creado exitosamente con ID: {} y QR: {}",
                savedTicket.getId(), savedTicket.getQrCode());

        return ticketMapper.toResponse(savedTicket);
    }

    /**
     * Cancela un ticket cambiando su status a CANCELLED.
     * No elimina físicamente el registro.
     */
    @Override
    public void deleteTicket(Long id) {
        log.info("Cancelando ticket ID: {}", id);

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

    /**
     * Obtiene un ticket por su identificador.
     */
    @Override
    @Transactional(readOnly = true)
    public TicketDtos.TicketResponse getTicket(Long id) {
        log.debug("Buscando ticket por ID: {}", id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ticket no encontrado con ID: {}", id);
                    return new NotFoundException(
                            String.format("Ticket con ID %d no encontrado", id)
                    );
                });

        return ticketMapper.toResponse(ticket);
    }

    /**
     * Obtiene todos los tickets de un viaje específico.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByTrip(Long tripId) {
        log.debug("Buscando tickets del trip ID: {}", tripId);

        List<Ticket> tickets = ticketRepository.findByTripId(tripId);

        log.info("Se encontraron {} tickets para el trip ID: {}", tickets.size(), tripId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los tickets de una compra específica.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByPurchase(Long purchaseId) {
        log.debug("Buscando tickets de la compra ID: {}", purchaseId);

        List<Ticket> tickets = ticketRepository.findByPurchaseId(purchaseId);

        log.info("Se encontraron {} tickets para la compra ID: {}", tickets.size(), purchaseId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los tickets de un pasajero específico.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketDtos.TicketResponse> getTicketsByPassenger(Long passengerId) {
        log.debug("Buscando tickets del pasajero ID: {}", passengerId);

        List<Ticket> tickets = ticketRepository.findByPassengerId(passengerId);

        log.info("Se encontraron {} tickets para el pasajero ID: {}", tickets.size(), passengerId);

        return tickets.stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Genera un código QR único para un ticket existente.
     * Útil si se necesita regenerar el QR
     */
    @Override
    public void generateQrForTicket(Long ticketId) {
        log.info("Generando QR para ticket ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.error("Ticket no encontrado con ID: {}", ticketId);
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

    /**
     * Valida un código QR de ticket.
     * Verifica que el QR exista y que el ticket tenga status SOLD
     */
    @Override
    @Transactional(readOnly = true)
    public void validateQrForTicket(String qrCode) {
        log.info("Validando QR: {}", qrCode);

        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> {
                    log.error("QR no encontrado: {}", qrCode);
                    return new NotFoundException(
                            String.format("Ticket con QR '%s' no encontrado", qrCode)
                    );
                });

        // Validar que el status sea SOLD
        if (ticket.getStatus() != Ticket.Status.SOLD) {
            log.warn("Intento de validar QR de ticket con status {}: {}",
                    ticket.getStatus(), qrCode);
            throw new InvalidCredentialsException(
                    String.format("El ticket no está activo (status: %s)", ticket.getStatus())
            );
        }

        log.info("QR validado exitosamente para ticket ID: {}", ticket.getId());
    }

    /**
     * Genera un código QR único usando UUID.
     * Formato: TICKET-{UUID}
     */
    private String generateUniqueQrCode() {
        return "TICKET-" + UUID.randomUUID().toString().toUpperCase();
    }
}