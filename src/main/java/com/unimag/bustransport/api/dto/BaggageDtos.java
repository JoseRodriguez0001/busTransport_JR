package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;

public class BaggageDtos {
    public record BaggageCreateRequest(
            @NotNull Long ticketId,
            @NotNull @Positive Double weightKg,
            String tagCode
    ) implements Serializable {}

    public record BaggageUpdateRequest(
            @Positive Double weightKg,
            String tagCode
    ) implements Serializable {}

    public record BaggageResponse(
            Long id,
            Double weightKg,
            BigDecimal fee,
            String tagCode,
            TicketSummary ticket
    ) implements Serializable {
        public record TicketSummary(Long id, String seatNumber, String qrCode) implements Serializable {}
    }
}
