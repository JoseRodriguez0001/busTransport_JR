package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Purchase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class PurchaseDtos {
    public record PurchaseCreateRequest(
            @NotNull @Positive BigDecimal totalAmount,
            @NotNull String paymentMethod,
            @NotNull Long userId
    ) implements Serializable {}

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
        public record UserSummary(Long id, String username, String email) implements Serializable {}
        public record TicketSummary(Long id, String seatNumber, BigDecimal price, String status) implements Serializable {}
    }
}
