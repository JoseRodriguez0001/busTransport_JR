package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class IncidentDtos {
    public record IncidentCreateRequest(
            @NotBlank String entityType,
            @NotNull Long entityId,
            @NotBlank String type,
            String note
    ) implements Serializable {}

    public record IncidentUpdateRequest(
            String type,
            String note
    ) implements Serializable {}

    public record IncidentResponse(
            Long id,
            String entityType,
            Long entityId,
            String type,
            String note,
            OffsetDateTime createdAt
    ) implements Serializable {}
}
