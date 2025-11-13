package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Purchase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class PurchaseDtos {
    public record PurchaseCreateRequest(
            @NotNull Long userId,
            @NotNull String paymentMethod,
            @NotEmpty List<TicketRequest> tickets
    ) implements Serializable {

        public record TicketRequest(
            @NotNull Long tripId,
            @NotNull Long passengerId,
            @NotBlank String seatNumber,
            @NotNull Long fromStopId,
            @NotNull Long toStopId,
            BaggageRequest baggage) implements Serializable {}

        public record BaggageRequest(
                @NotNull @Positive Double weightKg,
                String tagCode
        ) {}
    }

    public record PurchaseUpdateRequest(
            String paymentStatus
    ) implements Serializable {}

    public record PurchaseResponse(
            Long id,
            BigDecimal totalAmount,
            String paymentMethod,
            String paymentStatus,
            OffsetDateTime createdAt,
            UserSummary user,
            List<TicketSummary> tickets
    ) implements Serializable {
        public record UserSummary(Long id, String name, String email) implements Serializable {}
        public record TicketSummary(Long id, String seatNumber, BigDecimal price, String status) implements Serializable {}
    }
}
