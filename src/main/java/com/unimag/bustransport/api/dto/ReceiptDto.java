package com.unimag.bustransport.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record ReceiptDto(
        Long purchaseId,
        String receiptNumber,
        OffsetDateTime purchaseDate,
        BuyerInfo buyer,
        TripInfo trip,
        List<TicketInfo> tickets,
        BigDecimal subtotalTickets,
        BigDecimal subtotalBaggage,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentReference
) {
    public record BuyerInfo(
            String name,
            String email,
            String phone
    ) {}

    public record TripInfo(
            String routeName,
            String origin,
            String destination,
            OffsetDateTime departureAt,
            OffsetDateTime arrivalEta
    ) {}

    public record TicketInfo(
            String ticketNumber,
            String passengerName,
            String seatNumber,
            BigDecimal price,
            BaggageInfo baggage
    ) {}

    public record BaggageInfo(
            Double weightKg,
            BigDecimal fee
    ) {}
}