package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Bus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;

public class BusDtos {
    public record BusCreateRequest(
            @NotBlank String plate,
            @NotNull Integer capacity,
            List<String> amenities, // aire, wifi, baño, etc.
            @NotNull Bus.Status status
    ) implements Serializable {}
    public record BusUpdateRequest(
            Integer capacity,
            List<String> amenities,
            Bus.Status status
    ) implements Serializable {}
    public record BusResponse(
            Long id,
            String plate,
            Integer capacity,
            List<String> amenities,
            String status,
            Integer totalSeats
    ) implements Serializable {}

}
