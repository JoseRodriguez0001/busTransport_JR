package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class UserDtos {

    public record UserCreateRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String name,
            String phone
    ) implements Serializable {}

    public record EmployeeCreateRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            String phone,
            @NotNull Role role
    ) implements Serializable {}

    public record UserUpdateRequest(
            String name,
            String phone
    ) implements Serializable {}

    public record UserResponse(
            Long id,
            String email,
            String name,
            String phone,
            Role role,
            String status,
            OffsetDateTime createdAt
    ) implements Serializable {}
}
