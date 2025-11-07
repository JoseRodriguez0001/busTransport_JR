package com.unimag.bustransport.notification.impl;

import com.unimag.bustransport.notification.NotificationRequest;
import com.unimag.bustransport.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNotificationService implements NotificationService {

    @Override
    public final void send(NotificationRequest request) {
        try {
            validate(request);

            sendMessage(request);

            // Registrar envío exitoso
            logSuccess(request);

        } catch (Exception e) {
            log.error("Error enviando notificación {} a {}: {}",
                    getType(), request.recipient(), e.getMessage(), e);
            handleError(request, e);
        }
    }

    protected abstract void sendMessage(NotificationRequest request);

    protected void validate(NotificationRequest request) {
        if (request.recipient() == null || request.recipient().isBlank()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }

        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        if (request.type() != getType()) {
            throw new IllegalArgumentException(
                    String.format("Invalid notification type. Expected %s but got %s",
                            getType(), request.type())
            );
        }
    }

    protected void logSuccess(NotificationRequest request) {
        log.info("✅ Notificación {} enviada exitosamente a: {}",
                getType(), maskRecipient(request.recipient()));
    }

    protected void handleError(NotificationRequest request, Exception e) {
        log.error("❌ Fallo al enviar notificación {} a: {}. Error: {}",
                getType(), maskRecipient(request.recipient()), e.getMessage());

    }

    protected String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) {
            return "***";
        }

        int visibleChars = 3;
        String visible = recipient.substring(0, visibleChars);
        String masked = "*".repeat(recipient.length() - visibleChars);

        return visible + masked;
    }
}
