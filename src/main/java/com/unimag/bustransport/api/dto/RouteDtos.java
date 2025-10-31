package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.List;

public class RouteDtos {
    public record RouteCreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String origin,
            @NotBlank String destination,
            Double dictanceKm,
            Double durationMin
    ) implements Serializable {}
    public record RouteUpdateRequest(
            String name,
            String origin,
            String destination,
            Double distanceKm,
            Integer durationMin
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
