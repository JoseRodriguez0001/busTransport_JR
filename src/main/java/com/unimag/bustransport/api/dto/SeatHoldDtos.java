package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class SeatHoldDtos {
    public record SeatHoldCreateRequest(
            @NotBlank String seatNumber,
            @NotNull Long tripId,
            @NotNull Long userId
    ) implements Serializable {}

    public record SeatHoldResponse(
            Long id,
            String seatNumber,
            String status,
            OffsetDateTime expiresAt,
            TripSummary trip,
            UserSummary user
    ) implements Serializable {
        public record TripSummary(Long id, String origin, String destination, OffsetDateTime departureAt) implements Serializable {}
        public record UserSummary(Long id, String name) implements Serializable {}
    }
}
