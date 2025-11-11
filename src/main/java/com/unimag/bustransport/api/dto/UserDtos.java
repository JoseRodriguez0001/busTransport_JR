package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class UserDtos {

    //  Para registro público
    public record UserCreateRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String name,
            String phone
    ) implements Serializable {}

    // Para crear empleados (solo los crea un admin)
    public record EmployeeCreateRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            String phone,
            @NotNull Role role  // ROLE_ADMIN, ROLE_CLERK, ROLE_DRIVER, ROLE_DISPATCHER
    ) implements Serializable {}

    //  Para actualizar usuario (SIN rol, email, status, password)
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
