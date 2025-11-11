package com.unimag.bustransport.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.BaggageDtos;
import com.unimag.bustransport.api.dto.PurchaseDtos;
import com.unimag.bustransport.api.dto.ReceiptDto;
import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.*;
import com.unimag.bustransport.services.mapper.PurchaseMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PurchaseMapper purchaseMapper;
    private final SeatHoldService seatHoldService;
    private final FareRuleService fareRuleService;
    private final TicketService ticketService;

    @Override
    public PurchaseDtos.PurchaseResponse createPurchase(PurchaseDtos.PurchaseCreateRequest request) {

        // 1. Validar usuario
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("User with ID %d not found", request.userId())
                ));

        // 2. Validar que todos los tickets pertenezcan al mismo trip
        validateAllTicketsSameTrip(request.tickets());

        Long tripId = request.tickets().get(0).tripId();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        // 3. Validar que los SeatHolds estén activos
        List<String> seatNumbers = request.tickets().stream()
                .map(PurchaseDtos.PurchaseCreateRequest.TicketRequest::seatNumber)
                .collect(Collectors.toList());

        seatHoldService.validateActiveHolds(tripId, seatNumbers, user.getId());

        // 4. Crear Purchase en PENDING
        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.ZERO)
                .paymentMethod(Purchase.PaymentMethod.valueOf(request.paymentMethod()))
                .paymentStatus(Purchase.PaymentStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        purchaseRepository.save(purchase);

        // 5. Crear Tickets en estado PENDING y calcular total
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseDtos.PurchaseCreateRequest.TicketRequest ticketReq : request.tickets()) {

            // Calcular precio del ticket
            BigDecimal ticketPrice = fareRuleService.calculatePrice(
                    trip.getRoute().getId(),
                    ticketReq.fromStopId(),
                    ticketReq.toStopId(),
                    ticketReq.passengerId(),
                    trip.getBus().getId(),
                    ticketReq.seatNumber(),
                    ticketReq.tripId()
            );

            totalAmount = totalAmount.add(ticketPrice);

            // Crear ticket en PENDING (sin QR todavía)
            TicketDtos.TicketCreateRequest ticketCreateReq = new TicketDtos.TicketCreateRequest(
                    ticketReq.seatNumber(),
                    ticketPrice,
                    ticketReq.tripId(),
                    ticketReq.passengerId(),
                    ticketReq.fromStopId(),
                    ticketReq.toStopId(),
                    purchase.getId()
            );

            ticketService.createTicket(ticketCreateReq);

            log.info("Ticket PENDING created: seat={}, price={}",
                    ticketReq.seatNumber(), ticketPrice);
        }

        // 6. Actualizar total de la purchase (solo tickets, sin baggage)
        purchase.setTotalAmount(totalAmount);
        purchaseRepository.save(purchase);

        log.info("Purchase created with ID {} for user {} with total amount {} (PENDING)",
                purchase.getId(), user.getId(), totalAmount);

        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public PurchaseDtos.PurchaseResponse getPurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Purchase with ID %d not found", purchaseId)
                ));

        log.info("Purchase with ID {} retrieved", purchaseId);
        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public List<PurchaseDtos.PurchaseResponse> getPurchasesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with ID %d not found", userId));
        }

        List<Purchase> purchases = purchaseRepository.findByUserId(userId);

        log.info("Retrieved {} purchases for user ID {}", purchases.size(), userId);

        return purchases.stream()
                .map(purchaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void confirmPurchase(Long purchaseId, String paymentReference) {

        // 1. Obtener purchase
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Purchase with ID %d not found", purchaseId)
                ));

        // 2. Validar estado
        if (purchase.getPaymentStatus() != Purchase.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot confirm purchase with ID %d. Current status: %s",
                            purchaseId, purchase.getPaymentStatus())
            );
        }

        // 3. Validar que los SeatHolds TODAVÍA estén activos
        List<String> seatNumbers = purchase.getTickets().stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toList());

        Long tripId = purchase.getTickets().get(0).getTrip().getId();

        //todo averiguar por que aqui se usa try catch
        try {
            seatHoldService.validateActiveHolds(tripId, seatNumbers, purchase.getUser().getId());
        } catch (Exception e) {
            log.error("SeatHolds expired for purchase {}", purchaseId);
            throw new IllegalStateException(
                    "Cannot confirm purchase: seat holds have expired. Please reserve seats again."
            );
        }

        // 4. Actualizar Purchase a CONFIRMED
        purchase.setPaymentStatus(Purchase.PaymentStatus.CONFIRMED);
        purchase.setPaymentReference(paymentReference);

        // 5. Cambiar Tickets de PENDING a SOLD y generar QR usando TicketService
        purchase.getTickets().forEach(ticket -> {
            ticket.setStatus(Ticket.Status.SOLD);
            ticketService.generateQrForTicket(ticket.getId());

            log.info("Ticket ID {} changed from PENDING to SOLD with QR", ticket.getId());
        });

        purchaseRepository.save(purchase);

        log.info("Purchase {} confirmed with payment reference {} and {} tickets SOLD",
                purchaseId, paymentReference, purchase.getTickets().size());
    }

    @Override
    public void cancelPurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Purchase with ID %d not found", purchaseId)
                ));

        if (purchase.getPaymentStatus() == Purchase.PaymentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    String.format("Cannot cancel purchase with ID %d. Payment already confirmed", purchaseId)
            );
        }

        if (purchase.getPaymentStatus() == Purchase.PaymentStatus.CANCELLED) {
            throw new IllegalStateException(
                    String.format("Purchase with ID %d is already cancelled", purchaseId)
            );
        }

        purchase.setPaymentStatus(Purchase.PaymentStatus.CANCELLED);
        purchaseRepository.save(purchase);

        // Cancelar tickets asociados
        if (!purchase.getTickets().isEmpty()) {
            purchase.getTickets().forEach(ticket -> {
                ticket.setStatus(Ticket.Status.CANCELLED);
            });
        }

        log.info("Purchase with ID {} cancelled", purchaseId);
    }

    @Override
    public List<PurchaseDtos.PurchaseResponse> getPurchasesByDateRange(OffsetDateTime start, OffsetDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<Purchase> purchases = purchaseRepository.findByDateRange(start, end);

        log.info("Retrieved {} purchases between {} and {}", purchases.size(), start, end);

        return purchases.stream()
                .map(purchaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateAllTicketsSameTrip(List<PurchaseDtos.PurchaseCreateRequest.TicketRequest> tickets) {
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Purchase must contain at least one ticket");
        }

        Long firstTripId = tickets.get(0).tripId();
        boolean allSameTrip = tickets.stream()
                .allMatch(ticket -> ticket.tripId().equals(firstTripId));

        if (!allSameTrip) {
            throw new IllegalArgumentException("All tickets in a purchase must belong to the same trip");
        }
    }

}