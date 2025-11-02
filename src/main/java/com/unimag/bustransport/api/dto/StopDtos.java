package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class StopDtos {
    public record StopCreateRequest(
            @NotBlank String name,
            @NotNull Integer order,
            @NotNull Double lat,
            @NotNull Double lng,
            @NotNull Long routeId
    ) implements Serializable {}
    public record StopUpdateRequest(
            String name,
            Integer order,
            Double lat,
            Double lng
    ) implements Serializable {}
    public record StopResponse(
            Long id,
            String name,
            Integer order,
            Double lat,
            Double lng
    ) implements Serializable {}



}
