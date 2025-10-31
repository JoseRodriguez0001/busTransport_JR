package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class UserDtos {
    public record UserCreateRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String phone,
            @NotBlank String password
            ) implements Serializable{}
    public record UserUpdateRequest(String name,
                                    String phone,
                                    String password) implements Serializable{}
    public record UserResponse(Long id,
                               String name,
                               String email,
                               String phone,
                               Role role,
                               String status,
                               List<PassengerDtos.PassengerResponse> passengers
                               ) implements Serializable{}
}
