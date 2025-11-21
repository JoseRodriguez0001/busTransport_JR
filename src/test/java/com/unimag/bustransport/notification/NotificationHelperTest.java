package com.unimag.bustransport.notification;

import com.unimag.bustransport.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHelperTest {

    @Mock
    private NotificationFactory notificationFactory;

    @InjectMocks
    private NotificationHelper notificationHelper;

    private Route route;
    private Bus bus;
    private Trip trip;
    private User user;
    private Passenger passenger;
    private Stop fromStop;
    private Stop toStop;
    private Purchase purchase;
    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    void setUp() {
        route = createRoute(1L, "Bogotá", "Medellín");
        bus = createBus(1L, "ABC123", 40);
        trip = createTrip(1L, bus, route);
        user = createUser(1L, "test@example.com", "3001234567");
        passenger = createPassenger(1L, "Juan Pérez", "123456789", "3009876543");
        fromStop = createStop(1L, "Terminal Bogotá", 0, route);
        toStop = createStop(2L, "Terminal Medellín", 5, route);
        purchase = createPurchase(1L, user, BigDecimal.valueOf(150000), Purchase.PaymentStatus.CONFIRMED);
        ticket1 = createTicket(1L, "1A", trip, passenger, fromStop, toStop, purchase);
        ticket2 = createTicket(2L, "1B", trip, passenger, fromStop, toStop, purchase);
    }

    @Test
    @DisplayName("Debe enviar notificación de confirmación de compra correctamente")
    void testSendPurchaseConfirmation_Success() {
        // Given
        purchase.getTickets().add(ticket1);
        purchase.getTickets().add(ticket2);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        // When
        notificationHelper.sendPurchaseConfirmation(purchase, NotificationType.WHATSAPP);

        // Then
        verify(notificationFactory, times(1)).send(captor.capture());

        NotificationRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.type()).isEqualTo(NotificationType.WHATSAPP);
        assertThat(capturedRequest.message())
                .contains("Bogotá")
                .contains("Medellín")
                .contains("1A, 1B")
                .contains("150.000,00")
                .contains("PUR-1");
    }

    @Test
    @DisplayName("Debe enviar notificación de cancelación de ticket correctamente")
    void testCancelTicket_Success() {
        // Given
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        // When
        notificationHelper.cancelTicket(ticket1, NotificationType.SMS);

        // Then
        verify(notificationFactory, times(1)).send(captor.capture());

        NotificationRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.type()).isEqualTo(NotificationType.SMS);
        assertThat(capturedRequest.message()).contains("Ticket cancelled");
    }



    @Test
    @DisplayName("Debe enviar notificación con tipo SMS correctamente")
    void testSendNotification_WithSMSType() {
        // Given
        purchase.getTickets().add(ticket1);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        // When
        notificationHelper.sendPurchaseConfirmation(purchase, NotificationType.SMS);

        // Then
        verify(notificationFactory).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.SMS);
    }


    // Helper methods
    private Route createRoute(Long id, String origin, String destination) {
        Route route = new Route();
        route.setId(id);
        route.setCode("R001");
        route.setName(origin + " - " + destination);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistanceKm(400.0);
        route.setDurationMin(360);
        return route;
    }

    private Bus createBus(Long id, String plate, Integer capacity) {
        Bus bus = new Bus();
        bus.setId(id);
        bus.setPlate(plate);
        bus.setCapacity(capacity);
        bus.setStatus(Bus.Status.ACTIVE);
        return bus;
    }

    private Trip createTrip(Long id, Bus bus, Route route) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setDate(LocalDate.of(2025, 12, 25));
        trip.setBus(bus);
        trip.setRoute(route);
        trip.setDepartureAt(OffsetDateTime.parse("2025-12-25T08:00:00-05:00"));
        trip.setArrivalAt(OffsetDateTime.parse("2025-12-25T14:00:00-05:00"));
        trip.setStatus(Trip.Status.SCHEDULED);
        return trip;
    }

    private User createUser(Long id, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        user.setPhone(phone);
        user.setPasswordHash("hash");
        user.setRole(Role.ROLE_PASSENGER);
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    private Passenger createPassenger(Long id, String fullName, String documentNumber, String phone) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setFullName(fullName);
        passenger.setDocumentType("CC");
        passenger.setDocumentNumber(documentNumber);
        passenger.setBirthDate(LocalDate.of(1990, 5, 15));
        passenger.setPhoneNumber(phone);
        passenger.setCreatedAt(OffsetDateTime.now());
        return passenger;
    }

    private Stop createStop(Long id, String name, Integer order, Route route) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        stop.setOrder(order);
        stop.setLat(4.7110);
        stop.setLng(-74.0721);
        stop.setRoute(route);
        return stop;
    }

    private Purchase createPurchase(Long id, User user, BigDecimal totalAmount, Purchase.PaymentStatus paymentStatus) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setUser(user);
        purchase.setTotalAmount(totalAmount);
        purchase.setPaymentMethod(Purchase.PaymentMethod.CARD);
        purchase.setPaymentStatus(paymentStatus);
        purchase.setCreatedAt(OffsetDateTime.now());
        purchase.setTickets(new ArrayList<>()); // Inicializar lista
        return purchase;
    }

    private Ticket createTicket(Long id, String seatNumber, Trip trip, Passenger passenger,
                                Stop fromStop, Stop toStop, Purchase purchase) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(BigDecimal.valueOf(75000));
        ticket.setStatus(Ticket.Status.SOLD);
        ticket.setTrip(trip);
        ticket.setPassenger(passenger);
        ticket.setFromStop(fromStop);
        ticket.setToStop(toStop);
        ticket.setPurchase(purchase);
        return ticket;
    }
}
