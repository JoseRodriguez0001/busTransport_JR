package com.unimag.bustransport.notification;

import com.unimag.bustransport.domain.entities.Purchase;
import com.unimag.bustransport.domain.entities.Ticket;
import com.unimag.bustransport.domain.entities.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


 // Helper para construir y enviar notificaciones de forma simplificada

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHelper {

    private final NotificationFactory notificationFactory;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    public void sendPurchaseConfirmation(Purchase purchase, NotificationType type) {

        if (purchase.getTickets().isEmpty()) {
            log.warn("Purchase {} has no tickets, skipping notification", purchase.getId());
            return;
        }

        Ticket firstTicket = purchase.getTickets().get(0);
        Trip trip = firstTicket.getTrip();

        // Construir lista de asientos
        String seats = purchase.getTickets().stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.joining(", "));

        // Formatear mensaje usando el template
        String message = NotificationTemplate.PURCHASE_CONFIRMED.format(
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getDate().format(DATE_FORMATTER),
                trip.getDepartureAt().format(TIME_FORMATTER),
                seats,
                formatMoney(purchase.getTotalAmount()),
                "PUR-" + purchase.getId()
        );

        String phone = purchase.getUser().getPhone();

        // Enviar notificación
        NotificationRequest request = new NotificationRequest(phone, message, type);
        notificationFactory.send(request);
    }

    public void sendPlatformChange(Trip trip, String newPlatform, List<String> phones, NotificationType type) {

        String message = NotificationTemplate.PLATFORM_CHANGE.format(
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getDate().format(DATE_FORMATTER),
                newPlatform,
                trip.getDepartureAt().format(TIME_FORMATTER)
        );

        for (String phone : phones) {
            try {
                NotificationRequest request = new NotificationRequest(phone, message, type);
                notificationFactory.send(request);
            } catch (Exception e) {
                log.error("Failed to send platform change notification to {}: {}",
                        phone, e.getMessage());
            }
        }
    }

    public void sendArrivalSoon(Ticket ticket, String platform, NotificationType type) {

        Trip trip = ticket.getTrip();

        String message = NotificationTemplate.ARRIVAL_SOON.format(
                trip.getRoute().getDestination(),
                trip.getArrivalAt().format(TIME_FORMATTER),
                platform,
                ticket.getSeatNumber()
        );

        String phone = ticket.getPassenger().getPhoneNumber();

        NotificationRequest request = new NotificationRequest(phone, message, type);
        notificationFactory.send(request);
    }

    public void cancelTicket(Ticket ticket, NotificationType type) {

        String message = "Ticket cancelled";

        String phone = ticket.getPassenger().getPhoneNumber();

        NotificationRequest request = new NotificationRequest(phone, message, type);
        notificationFactory.send(request);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}