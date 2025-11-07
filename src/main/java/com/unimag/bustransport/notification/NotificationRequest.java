package com.unimag.bustransport.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

 //DTO para solicitudes de notificación

public record NotificationRequest(
        @NotBlank String recipient,
        @NotBlank String message,
        @NotNull NotificationType type,
        String subject // Opcional, solo para emails
) implements Serializable {
    public NotificationRequest(String recipient, String message, NotificationType type) {
        this(recipient, message, type, null);
    }
}
