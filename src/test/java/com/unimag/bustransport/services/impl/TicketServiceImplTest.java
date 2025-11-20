package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.InvalidCredentialsException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private StopRepository stopRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Spy
    private TicketMapper ticketMapper = Mappers.getMapper(TicketMapper.class);
    @InjectMocks
    private TicketServiceImpl ticketService;

    private Route route;
    private Bus bus;
    private Trip trip;
    private Passenger passenger;
    private Stop fromStop;
    private Stop toStop;
    private Purchase purchase;
    private User user;
    private Ticket ticket;
    private TicketDtos.TicketCreateRequest createRequest;

    @BeforeEach
    void setUp() {

        user = createUser(1L, "test@example.com", "Test User");
        route = createRoute(1L, "R001", "Santa Marta", "Barranquilla", 100.0, 120);
        bus = createBus(1L, "ABC123", 40);
        trip = createTrip(1L, bus, route);
        passenger = createPassenger(1L, "Juan Pérez", "123456789");
        fromStop = createStop(1L, "Terminal Santa Marta", 0, route);
        toStop = createStop(2L, "Terminal Barranquilla", 5, route);
        purchase = createPurchase(1L, user, BigDecimal.valueOf(50000), Purchase.PaymentStatus.CONFIRMED);
        ticket = createTicket(1L, "1A", trip, passenger, fromStop, toStop, purchase);

        createRequest = new TicketDtos.TicketCreateRequest(
                "1A",
                BigDecimal.valueOf(50000),
                1L,
                1L,
                1L,
                2L,
                1L
        );
    }

    @Test
    @DisplayName("Debe crear un ticket exitosamente")
    void createTicket_ShouldCreateSuccessfully() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(ticketRepository.findOverlappingTickets(1L, "1A", 0, 5))
                .thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        TicketDtos.TicketResponse response = ticketService.createTicket(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.seatNumber()).isEqualTo("1A");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.qrCode()).isNull(); // QR no se genera en creación

        verify(tripRepository).findById(1L);
        verify(passengerRepository).findById(1L);
        verify(stopRepository).findById(1L);
        verify(stopRepository).findById(2L);
        verify(purchaseRepository).findById(1L);
        verify(ticketRepository).findOverlappingTickets(1L, "1A", 0, 5);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el trip no existe")
    void createTicket_ShouldThrowNotFoundException_WhenTripDoesNotExist() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(createRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trip with ID 1 not found");

        verify(tripRepository).findById(1L);
        verify(ticketRepository, never()).save(any());
    }


    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si el asiento está ocupado en el tramo")
    void createTicket_ShouldThrowDuplicateException_WhenSeatAlreadyOccupied() {
        // Given
        Ticket existingTicket = createTicket(2L, "1A", trip, passenger, fromStop, toStop, purchase);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(ticketRepository.findOverlappingTickets(1L, "1A", 0, 5))
                .thenReturn(List.of(existingTicket));

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("El asiento 1A ya está ocupado en el tramo seleccionado");

        verify(ticketRepository, never()).save(any());
    }


    @Test
    @DisplayName("Debe cancelar un ticket exitosamente")
    void deleteTicket_ShouldCancelSuccessfully() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        // When
        ticketService.deleteTicket(1L);

        // Then
        verify(ticketRepository).findById(1L);
        verify(ticketRepository).save(ticket);
        assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.CANCELLED);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el ticket no existe")
    void deleteTicket_ShouldThrowNotFoundException_WhenTicketDoesNotExist() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.deleteTicket(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket con ID 1 no encontrado");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener un ticket por ID")
    void getTicket_ShouldReturnTicket() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // When
        TicketDtos.TicketResponse response = ticketService.getTicket(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.seatNumber()).isEqualTo("1A");

        verify(ticketRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe obtener todos los tickets de un trip")
    void getTicketsByTrip_ShouldReturnAllTickets() {
        // Given
        Ticket ticket2 = createTicket(2L, "1B", trip, passenger, fromStop, toStop, purchase);
        Ticket ticket3 = createTicket(3L, "1C", trip, passenger, fromStop, toStop, purchase);

        when(ticketRepository.findByTripId(1L))
                .thenReturn(List.of(ticket, ticket2, ticket3));

        // When
        List<TicketDtos.TicketResponse> responses = ticketService.getTicketsByTrip(1L);

        // Then
        assertThat(responses)
                .hasSize(3)
                .extracting(TicketDtos.TicketResponse::seatNumber)
                .containsExactly("1A", "1B", "1C");

        verify(ticketRepository).findByTripId(1L);
    }

    @Test
    @DisplayName("Debe obtener todos los tickets de una compra")
    void getTicketsByPurchase_ShouldReturnAllTickets() {
        // Given
        Ticket ticket2 = createTicket(2L, "1B", trip, passenger, fromStop, toStop, purchase);

        when(ticketRepository.findByPurchaseId(1L))
                .thenReturn(List.of(ticket, ticket2));

        // When
        List<TicketDtos.TicketResponse> responses = ticketService.getTicketsByPurchase(1L);

        // Then
        assertThat(responses)
                .hasSize(2)
                .extracting(TicketDtos.TicketResponse::seatNumber)
                .containsExactly("1A", "1B");

        verify(ticketRepository).findByPurchaseId(1L);
    }

    @Test
    @DisplayName("Debe obtener todos los tickets de un pasajero")
    void getTicketsByPassenger_ShouldReturnAllTickets() {
        // Given
        Ticket ticket2 = createTicket(2L, "2A", trip, passenger, fromStop, toStop, purchase);
        Ticket ticket3 = createTicket(3L, "3A", trip, passenger, fromStop, toStop, purchase);

        when(ticketRepository.findByPassengerId(1L))
                .thenReturn(List.of(ticket, ticket2, ticket3));

        // When
        List<TicketDtos.TicketResponse> responses = ticketService.getTicketsByPassenger(1L);

        // Then
        assertThat(responses)
                .hasSize(3)
                .extracting(TicketDtos.TicketResponse::seatNumber)
                .containsExactly("1A", "2A", "3A");

        verify(ticketRepository).findByPassengerId(1L);
    }

    @Test
    @DisplayName("Debe generar QR para un ticket exitosamente")
    void generateQrForTicket_ShouldGenerateQrSuccessfully() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        // When
        ticketService.generateQrForTicket(1L);

        // Then
        verify(ticketRepository).findById(1L);
        verify(ticketRepository).save(ticket);
        assertThat(ticket.getQrCode()).isNotNull();
        assertThat(ticket.getQrCode()).startsWith("TICKET-");
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el ticket no existe")
    void generateQrForTicket_ShouldThrowNotFoundException_WhenTicketDoesNotExist() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.generateQrForTicket(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket con ID 1 no encontrado");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe validar QR exitosamente para ticket con status SOLD")
    void validateQrForTicket_ShouldValidateSuccessfully() {
        // Given
        ticket.setQrCode("TICKET-ABC123");
        ticket.setStatus(Ticket.Status.SOLD);
        when(ticketRepository.findByQrCode("TICKET-ABC123")).thenReturn(Optional.of(ticket));

        // When & Then
        assertThatCode(() -> ticketService.validateQrForTicket("TICKET-ABC123"))
                .doesNotThrowAnyException();

        verify(ticketRepository).findByQrCode("TICKET-ABC123");
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el QR no existe")
    void validateQrForTicket_ShouldThrowNotFoundException_WhenQrNotFound() {
        // Given
        when(ticketRepository.findByQrCode("INVALID-QR")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.validateQrForTicket("INVALID-QR"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket con QR 'INVALID-QR' no encontrado");
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException si el ticket no está en status SOLD")
    void validateQrForTicket_ShouldThrowInvalidCredentialsException_WhenStatusNotSold() {
        // Given
        ticket.setQrCode("TICKET-ABC123");
        ticket.setStatus(Ticket.Status.PENDING);
        when(ticketRepository.findByQrCode("TICKET-ABC123")).thenReturn(Optional.of(ticket));

        // When & Then
        assertThatThrownBy(() -> ticketService.validateQrForTicket("TICKET-ABC123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("El ticket no está activo (status: PENDING)");
    }

    @Test
    @DisplayName("Debe hacer reembolso de ticket")
    void refundTicketSuccesfully() {
        Ticket ticket = createTicket(1L,"5A",trip,passenger,fromStop,toStop,purchase);
        ticket.setStatus(Ticket.Status.SOLD);
        //TODO
    }
    @Test
    @DisplayName("Debe limpiar tickets PENDING expirados exitosamente")
    void expireOldTickets_ShouldCleanupExpiredTickets() {
        // Given
        Ticket expiredTicket1 = createTicket(2L, "2A", trip, passenger, fromStop, toStop, purchase);
        Ticket expiredTicket2 = createTicket(3L, "2B", trip, passenger, fromStop, toStop, purchase);
        expiredTicket1.setStatus(Ticket.Status.PENDING);
        expiredTicket2.setStatus(Ticket.Status.PENDING);

        when(ticketRepository.findExpiredPendingTickets(any(OffsetDateTime.class)))
                .thenReturn(List.of(expiredTicket1, expiredTicket2));

        // When
        int deletedCount = ticketService.expireOldTickets();

        // Then
        assertThat(deletedCount).isEqualTo(2);
        verify(ticketRepository).findExpiredPendingTickets(any(OffsetDateTime.class));
        verify(ticketRepository).deleteAll(List.of(expiredTicket1, expiredTicket2));
    }

    @Test
    @DisplayName("Debe retornar 0 cuando no hay tickets expirados")
    void expireOldTickets_ShouldReturnZero_WhenNoExpiredTickets() {
        // Given
        when(ticketRepository.findExpiredPendingTickets(any(OffsetDateTime.class)))
                .thenReturn(List.of());

        // When
        int deletedCount = ticketService.expireOldTickets();

        // Then
        assertThat(deletedCount).isEqualTo(0);
        verify(ticketRepository).findExpiredPendingTickets(any(OffsetDateTime.class));
        verify(ticketRepository, never()).deleteAll(any());
    }

    private User createUser(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setPhone("+573001234567");
        user.setPasswordHash("hash");
        user.setRole(Role.ROLE_PASSENGER);
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    private Route createRoute(Long id, String code, String origin, String destination, 
                              Double distanceKm, Integer durationMin) {
        Route route = new Route();
        route.setId(id);
        route.setCode(code);
        route.setName(origin + " - " + destination);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistanceKm(distanceKm);
        route.setDurationMin(durationMin);
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
        trip.setDate(LocalDate.now().plusDays(1));
        trip.setBus(bus);
        trip.setRoute(route);
        trip.setDepartureAt(OffsetDateTime.now().plusDays(1));
        trip.setArrivalAt(OffsetDateTime.now().plusDays(1).plusHours(2));
        trip.setOverbookingPercent(0.0);
        trip.setStatus(Trip.Status.SCHEDULED);
        return trip;
    }

    private Passenger createPassenger(Long id, String fullName, String documentNumber) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setFullName(fullName);
        passenger.setDocumentType("CC");
        passenger.setDocumentNumber(documentNumber);
        passenger.setBirthDate(LocalDate.of(1990, 5, 15));
        passenger.setPhoneNumber("+573001234567");
        passenger.setCreatedAt(OffsetDateTime.now());
        return passenger;
    }

    private Stop createStop(Long id, String name, Integer order, Route route) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        stop.setOrder(order);
        stop.setLat(11.2408);
        stop.setLng(-74.1990);
        stop.setRoute(route);
        return stop;
    }

    private Purchase createPurchase(Long id, User user, BigDecimal totalAmount, 
                                    Purchase.PaymentStatus paymentStatus) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setUser(user);
        purchase.setTotalAmount(totalAmount);
        purchase.setPaymentMethod(Purchase.PaymentMethod.CARD);
        purchase.setPaymentStatus(paymentStatus);
        purchase.setCreatedAt(OffsetDateTime.now());
        return purchase;
    }

    private Ticket createTicket(Long id, String seatNumber, Trip trip, Passenger passenger,
                                Stop fromStop, Stop toStop, Purchase purchase) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(BigDecimal.valueOf(50000));
        ticket.setStatus(Ticket.Status.PENDING);
        ticket.setTrip(trip);
        ticket.setPassenger(passenger);
        ticket.setFromStop(fromStop);
        ticket.setToStop(toStop);
        ticket.setPurchase(purchase);
        return ticket;
    }
}
