package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TicketDtos {
    public record TicketCreateRequest(
            @NotBlank String seatNumber,
            @NotNull BigDecimal price,
            @NotNull Long tripId,
            @NotNull Long passengerId,
            @NotNull Long fromStopId,
            @NotNull Long toStopId,
            @NotNull Long purchaseId
    ) implements Serializable {}

    public record TicketUpdateRequest(
            String status,
            String qrCode
    ) implements Serializable {}

    public record TicketResponse(
            Long id,
            String seatNumber,
            BigDecimal price,
            String status,
            String qrCode,
            TripSummary trip,
            PassengerSummary passenger,
            StopSummary fromStop,
            StopSummary toStop,
            PurchaseSummary purchase
    ) implements Serializable {
        public record TripSummary(Long id, String origin, String destination, OffsetDateTime departureAt) implements Serializable {}
        public record PassengerSummary(Long id, String fullName, String documentNumber) implements Serializable {}
        public record StopSummary(Long id, String name) implements Serializable {}
        public record PurchaseSummary(Long id, BigDecimal totalAmount, String paymentStatus) implements Serializable {}
    }

    public record ValidationResponse(boolean valid, String message) implements Serializable {}
}
