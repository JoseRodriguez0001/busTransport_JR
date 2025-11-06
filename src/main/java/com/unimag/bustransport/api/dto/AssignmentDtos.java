package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class AssignmentDtos {
    public record AssignmentCreateRequest(
            @NotNull Long tripId,
            @NotNull Long driverId,
            @NotNull Long dispatcherId,
            Boolean checklistOk
    ) implements Serializable {}

    public record AssignmentUpdateRequest(
            Boolean checklistOk
    ) implements Serializable {}

    public record AssignmentResponse(
            Long id,
            Boolean checklistOk,
            OffsetDateTime assignedAt,
            TripSummary trip,
            UserSummary driver,
            UserSummary dispatcher
    ) {
        public record TripSummary(Long id, String origin, String destination, OffsetDateTime departureAt) {}
        public record UserSummary(Long id, String name) {}
    }
}
