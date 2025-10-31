package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Seat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class SeatDtos {
    public record SeatCreateRequest(
            @NotBlank String number,
            @NotNull Seat.Type type,
            @NotNull Long busId
    ) implements Serializable {}
    public record SeatUpdateRequest(
            String number,
            Seat.Type type
    ) implements Serializable {}
    public record SeatResponse(
            Long id,
            String number,
            String type,
            Long busId,
            String busPlate,
            Boolean occupied
    ) implements Serializable {}


}
