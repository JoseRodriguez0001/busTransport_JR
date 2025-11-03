package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class UserDtos {
    public record UserCreateRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @Pattern(regexp = "^\\+?[0-9]{10,15}$")
            @NotBlank String phone,
            @Size(min = 8, max = 100)
            @NotBlank String password,
            @NotNull Role role
    ) implements Serializable{}
    public record UserUpdateRequest(
            String name,
            String phone,
            String password
    ) implements Serializable{}
    public record UserResponse(
            Long id,
            String name,
            String email,
            String phone,
            Role role,
            String status,
            @Nullable List<PassengerDtos.PassengerResponse> passengers,
            OffsetDateTime createAt
    ) implements Serializable{}
}
