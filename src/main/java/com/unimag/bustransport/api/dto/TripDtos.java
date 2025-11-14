package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Trip;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class TripDtos {
    public record TripCreateRequest(
            @NotNull Long routeId,
            @NotNull Long busId,
            @NotNull LocalDate date,
            @NotNull OffsetDateTime departureAt,
            @NotNull OffsetDateTime arrivalAt,
            Double overbookingPercent
    ) implements Serializable {}
    public record TripUpdateRequest(
            OffsetDateTime departureAt,
            OffsetDateTime arrivalAt,
            Double overbookingPercent,
            Trip.Status status
    ) implements Serializable {}
    public record TripResponse(
            Long id,

            RouteSummary route,
            BusSummary bus,
            LocalDate date,
            OffsetDateTime departureAt,
            OffsetDateTime arrivalAt,
            Integer durationMinutes,
            Double overbookingPercent,
            String status,
            Integer soldSeats,
            Integer availableSeats
    ) implements Serializable {

        public record RouteSummary(Long id, String code, String origin, String destination) implements Serializable {}
        public record BusSummary(Long id, String plate, Integer capacity) implements Serializable {}
    }

    public record StatisticsResponse(Long soldSeats) implements Serializable {}

}
