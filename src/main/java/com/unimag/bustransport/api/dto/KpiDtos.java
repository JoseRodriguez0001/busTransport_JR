package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;

public class KpiDtos {
    public record KpiCreateRequest(
            @NotBlank String name,
            @NotNull Double value
    ) implements Serializable {}

    public record KpiUpdateRequest(
            Double value
    ) implements Serializable {}

    public record KpiResponse(
            Long id,
            String name,
            Double value,
            OffsetDateTime calculatedAt
    ) implements Serializable {}
}

