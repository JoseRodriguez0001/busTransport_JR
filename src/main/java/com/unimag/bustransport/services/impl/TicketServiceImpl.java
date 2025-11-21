package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.InvalidCredentialsException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.notification.NotificationHelper;
import com.unimag.bustransport.notification.NotificationType;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.TicketService;
import com.unimag.bustransport.services.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private final ConfigService configService;
    private final TicketMapper ticketMapper;

    private static final int TICKET_CLEANUP_MINUTES = 15;
    private final NotificationHelper notificationHelper;

    @Override
    public TicketDtos.TicketResponse createTicket(TicketDtos.TicketCreateRequest request) {

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

        if (fromStop.getOrder() >= toStop.getOrder()) {
            log.error("The order of the origin stop ({}) must be less than the destination ({})",
                    fromStop.getOrder(), toStop.getOrder());
            throw new IllegalArgumentException(
                    "The origin stop must be before the destination stop on the route"
            );
        }

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


        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setTrip(trip);
        ticket.setPassenger(passenger);
        ticket.setFromStop(fromStop);
        ticket.setToStop(toStop);
        ticket.setPurchase(purchase);
        ticket.setStatus(Ticket.Status.PENDING);

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

        ticket.setStatus(Ticket.Status.CANCELLED);
        ticketRepository.save(ticket);

        log.info("Ticket ID: {} cancelado exitosamente", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDtos.TicketResponse getTicket(Long id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Ticket con ID %d no encontrado", id)
                ));

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
                .orElseThrow(() -> new NotFoundException(
                        String.format("Ticket con ID %d no encontrado", ticketId)
                ));

        String newQrCode = generateUniqueQrCode();
        ticket.setQrCode(newQrCode);
        ticketRepository.save(ticket);

        log.info("QR generado exitosamente para ticket ID: {} - QR: {}", ticketId, newQrCode);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateQrForTicket(String qrCode) {

        Ticket ticket = ticketRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Ticket con QR '%s' no encontrado", qrCode)
                ));

        if (ticket.getStatus() != Ticket.Status.SOLD) {
            throw new InvalidCredentialsException(
                    String.format("El ticket no está activo (status: %s)", ticket.getStatus())
            );
        }

        ticket.setStatus(Ticket.Status.BOARDED);
        ticketRepository.save(ticket);
        log.info("QR validated successfully for ticket ID: {}", ticket.getId());
    }

    @Override
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void processNoshows() {
        try {
            OffsetDateTime threshold = OffsetDateTime.now().plusMinutes(5);
            List<Ticket> tickets = ticketRepository.findTicketNoShow(threshold);

            for (Ticket ticket : tickets) {
                try {

                    if (ticket.getStatus() != Ticket.Status.SOLD) {
                        log.debug("Ticket {} status is {} -> skipping", ticket.getId(), ticket.getStatus());
                        continue;
                    }

                    ticket.setStatus(Ticket.Status.NO_SHOW);

                    ticketRepository.save(ticket);

                } catch (Exception e) {
                    log.error("Failed to process ticket noshow", e);
                }
            }
        }catch (Exception e) {
            log.error("Error processing ", e);
        }
    }

    @Override
    public void refundTicket(Long ticketId, Long userId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(()-> new NotFoundException(String.format("Ticket with ID %d not found", ticketId)));

        if (!ticket.getPurchase().getUser().getId().equals(userId)) {
            throw new InvalidCredentialsException("You can´t refund this Ticket");
        }

        if (ticket.getStatus() != Ticket.Status.SOLD) {
            throw new InvalidCredentialsException("Ticket status is invalid");
        }

        Trip trip = ticket.getTrip();
        if (trip.getDepartureAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("You can´t refund past trips");
        }

        long minutesDiff = Duration
                .between(OffsetDateTime.now(), trip.getDepartureAt())
                .toMinutes();

        BigDecimal refundPercent;
        if (minutesDiff >= 24 * 60) {
            refundPercent = configService.getValueAsBigDecimal("refund.>24");
        } else if (minutesDiff >= 2 * 60) {
            refundPercent = configService.getValueAsBigDecimal("refund.2to24");
        } else {
            refundPercent = configService.getValueAsBigDecimal("refund.<2");
        }

        BigDecimal refundAmount = ticket.getPrice().multiply(refundPercent);

        Purchase purchase = ticket.getPurchase();
        purchase.setTotalAmount(purchase.getTotalAmount().subtract(refundAmount));
        purchaseRepository.save(purchase);

        ticket.setStatus(Ticket.Status.CANCELLED);
        ticketRepository.save(ticket);

        try {
        notificationHelper.cancelTicket(ticket, NotificationType.WHATSAPP);
        } catch (Exception e){
            log.error("Error canceling ticket notification", e);
        }

        log.info("Ticket refunded successfully for ticket ID: {}", ticket.getId());
    }



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