package com.unimag.bustransport.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            @NotBlank(message = "Password is required")
            String password
    ) implements Serializable {}

     public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserInfo user
    ) implements Serializable {

        public record UserInfo(
                Long id,
                String email,
                String name,
                String role
        ) implements Serializable {}
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) implements Serializable {}
    public record RefreshTokenResponse(
            String accessToken,
            String tokenType,
            Long expiresIn
    ) implements Serializable {}
}
