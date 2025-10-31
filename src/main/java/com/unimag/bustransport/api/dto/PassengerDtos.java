package com.unimag.bustransport.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PassengerDtos {
    public record PassengerCreateRequest(
            @NotBlank String fullname,
            @NotBlank String documentType,
            @NotBlank String documentNumber,
            LocalDate birthDate,
            @NotBlank String phoneNumber
    ) implements Serializable {}
    public record PassengerUpdateRequest(
            String fullname,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phoneNumber) implements Serializable {}
    public record PassengerResponse(
            Long id,
            String fullname,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phoneNumber
    ) implements Serializable {}
}
