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
import java.util.ArrayList;
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
    private final BaggageService baggageService;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @Override
    public PurchaseDtos.PurchaseResponse createPurchase(PurchaseDtos.PurchaseCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("User with ID %d not found", request.userId())
                ));

        validateAllTicketsSameTrip(request.tickets());

        Long tripId = request.tickets().get(0).tripId();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        List<String> seatNumbers = request.tickets().stream()
                .map(PurchaseDtos.PurchaseCreateRequest.TicketRequest::seatNumber)
                .collect(Collectors.toList());

        seatHoldService.validateActiveHolds(tripId, seatNumbers, user.getId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TicketPriceInfo> ticketPrices = new ArrayList<>();

        for (PurchaseDtos.PurchaseCreateRequest.TicketRequest ticketReq : request.tickets()) {
            BigDecimal ticketPrice = fareRuleService.calculatePrice(
                    trip.getRoute().getId(),
                    ticketReq.fromStopId(),
                    ticketReq.toStopId(),
                    ticketReq.passengerId(),
                    trip.getBus().getId(),
                    ticketReq.seatNumber()
            );

            totalAmount = totalAmount.add(ticketPrice);

            BigDecimal baggageFee = BigDecimal.ZERO;
            if (ticketReq.baggage() != null) {
                baggageFee = baggageService.calculateBaggageFee(ticketReq.baggage().weightKg());
                totalAmount = totalAmount.add(baggageFee);
            }

            ticketPrices.add(new TicketPriceInfo(
                    ticketReq.seatNumber(),
                    ticketPrice,
                    baggageFee
            ));

            log.info("Ticket price calculated: seat={}, price={}, baggageFee={}",
                    ticketReq.seatNumber(), ticketPrice, baggageFee);
        }

        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(totalAmount)
                .paymentMethod(Purchase.PaymentMethod.valueOf(request.paymentMethod()))
                .paymentStatus(Purchase.PaymentStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        purchaseRepository.save(purchase);

        try {
            PurchaseMetadata metadata = new PurchaseMetadata(request.tickets(), ticketPrices);
            purchase.setMetadataJson(objectMapper.writeValueAsString(metadata));
            purchaseRepository.save(purchase);
        } catch (Exception e) {
            log.error("Error serializing purchase metadata", e);
            throw new RuntimeException("Failed to save purchase metadata");
        }

        log.info("Purchase created with ID {} for user {} with total amount {}",
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
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Purchase with ID %d not found", purchaseId)
                ));

        if (purchase.getPaymentStatus() != Purchase.PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot confirm purchase with ID %d. Current status: %s",
                            purchaseId, purchase.getPaymentStatus())
            );
        }

        PurchaseMetadata metadata;
        try {
            metadata = objectMapper.readValue(
                    purchase.getMetadataJson(),
                    PurchaseMetadata.class
            );
        } catch (Exception e) {
            log.error("Error deserializing purchase metadata", e);
            throw new RuntimeException("Failed to read purchase metadata");
        }

        List<String> seatNumbers = metadata.tickets().stream()
                .map(PurchaseDtos.PurchaseCreateRequest.TicketRequest::seatNumber)
                .collect(Collectors.toList());

        Long tripId = metadata.tickets().get(0).tripId();

        seatHoldService.validateActiveHolds(tripId, seatNumbers, purchase.getUser().getId());

        purchase.setPaymentStatus(Purchase.PaymentStatus.CONFIRMED);
        purchase.setPaymentReference(paymentReference);
        purchaseRepository.save(purchase);

        for (PurchaseDtos.PurchaseCreateRequest.TicketRequest ticketReq : metadata.tickets()) {
            BigDecimal ticketPrice = getPriceForSeat(ticketReq.seatNumber(), metadata.ticketPrices());

            TicketDtos.TicketCreateRequest ticketCreateReq = new TicketDtos.TicketCreateRequest(
                    ticketReq.seatNumber(),
                    ticketPrice,
                    ticketReq.tripId(),
                    ticketReq.passengerId(),
                    ticketReq.fromStopId(),
                    ticketReq.toStopId(),
                    purchase.getId()
            );

            TicketDtos.TicketResponse ticket = ticketService.createTicket(ticketCreateReq);

            if (ticketReq.baggage() != null) {
                BaggageDtos.BaggageCreateRequest baggageReq = new BaggageDtos.BaggageCreateRequest(
                        ticket.id(),
                        ticketReq.baggage().weightKg()
                );

                baggageService.registerBaggage(baggageReq);
            }

            log.info("Ticket created with ID {} for seat {}", ticket.id(), ticketReq.seatNumber());
        }

        seatHoldService.deleteHoldsByTripAndSeats(tripId, seatNumbers, purchase.getUser().getId());

        log.info("Purchase {} confirmed with payment reference {}", purchaseId, paymentReference);
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

        if (!purchase.getTickets().isEmpty()) {
            purchase.getTickets().forEach(ticket -> {
                ticketService.deleteTicket(ticket.getId());
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

    private BigDecimal getPriceForSeat(String seatNumber, List<TicketPriceInfo> ticketPrices) {
        return ticketPrices.stream()
                .filter(info -> info.seatNumber().equals(seatNumber))
                .findFirst()
                .map(TicketPriceInfo::ticketPrice)
                .orElseThrow(() -> new IllegalStateException("Price not found for seat " + seatNumber));
    }

    private record PurchaseMetadata(
            List<PurchaseDtos.PurchaseCreateRequest.TicketRequest> tickets,
            List<TicketPriceInfo> ticketPrices
    ) {}

    private record TicketPriceInfo(
            String seatNumber,
            BigDecimal ticketPrice,
            BigDecimal baggageFee
    ) {}
}