package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

public class RouteDtos {
    public record RouteCreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String origin,
            @NotBlank String destination,
            @Positive @NotNull Double distanceKm,
            @Positive @NotNull Integer durationMin
    ) implements Serializable {}
    public record RouteUpdateRequest(
            String name,
            String origin,
            String destination,
            @Positive Double distanceKm,
            @Positive Integer durationMin
    ) implements Serializable {}
    public record RouteResponse(
            Long id,
            String code,
            String name,
            String origin,
            String destination,
            Double distanceKm,
            Integer durationMin,
            List<StopDtos.StopResponse> stops,
            List<FareRuleDtos.FareRuleResponse> fareRules
    ) implements Serializable {}
}
