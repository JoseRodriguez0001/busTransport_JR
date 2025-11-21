package com.unimag.bustransport.notification.impl;

import com.unimag.bustransport.notification.NotificationRequest;
import com.unimag.bustransport.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppNotificationServiceImpl extends AbstractNotificationService {

    @Override
    protected void sendMessage(NotificationRequest request) {

        log.info("╔═══════════════════════════════════════════════════════╗");
        log.info("║          📱 WHATSAPP NOTIFICATION (MOCK)              ║");
        log.info("╠═══════════════════════════════════════════════════════╣");
        log.info("║ Para: {}", String.format("%-44s", request.recipient()) + "║");
        log.info("╠═══════════════════════════════════════════════════════╣");

        // Dividir mensaje en líneas para mejor visualización
        String[] lines = request.message().split("\n");
        for (String line : lines) {
            log.info("║ {}", String.format("%-52s", line) + "║");
        }

        log.info("╚═══════════════════════════════════════════════════════╝");

        simulateNetworkDelay();

    }

    @Override
    public NotificationType getType() {
        return NotificationType.WHATSAPP;
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(500); // 500ms de delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}