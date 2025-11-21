package com.unimag.bustransport.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;

public class PassengerDtos {
    public record PassengerCreateRequest(
            @NotBlank String fullName,
            @NotBlank String documentType,
            @NotBlank String documentNumber,
            @Past
            LocalDate birthDate,
            @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "El teléfono debe tener entre 10 y 15 dígitos")
            @NotBlank String phoneNumber,
            @Nullable
            Long userId
    ) implements Serializable {}
    public record PassengerUpdateRequest(
            String fullName,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phoneNumber) implements Serializable {}
    public record PassengerResponse(
            Long id,
            String fullName,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phoneNumber
    ) implements Serializable {}
}
