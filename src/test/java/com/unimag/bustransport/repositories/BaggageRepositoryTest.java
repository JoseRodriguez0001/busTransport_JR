package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BaggageRepositoryTest extends AbstractRepositoryTI {
    @Autowired
    private BaggageRepository baggageRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private BusRepository busRepository;

    @BeforeEach
    void setUp() {
        baggageRepository.deleteAll();
        ticketRepository.deleteAll();
        purchaseRepository.deleteAll();
        passengerRepository.deleteAll();
        tripRepository.deleteAll();
        stopRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Ticket givenTicket() {
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hash123")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        userRepository.save(user);

        Route route = Route.builder()
                .code("R001")
                .name("Route 1")
                .origin("City A")
                .destination("City B")
                .distanceKm(100.0)
                .durationMin(120)
                .build();
        routeRepository.save(route);

        Stop fromStop = Stop.builder()
                .name("Stop A")
                .order(1)
                .lat(10.0)
                .lng(-74.0)
                .route(route)
                .build();
        stopRepository.save(fromStop);

        Stop toStop = Stop.builder()
                .name("Stop B")
                .order(2)
                .lat(11.0)
                .lng(-75.0)
                .route(route)
                .build();
        stopRepository.save(toStop);

        Bus bus = Bus.builder()
                .plate("ABC123")
                .capacity(40)
                .amenities(new ArrayList<>())
                .status(Bus.Status.ACTIVE)
                .build();
        busRepository.save(bus);

        Trip trip = Trip.builder()
                .route(route)
                .bus(bus)
                .date(LocalDate.now().plusDays(1))
                .departureAt(OffsetDateTime.now().plusDays(1))
                .arrivalAt(OffsetDateTime.now().plusDays(1).plusHours(2))
                .status(Trip.Status.SCHEDULED)
                .build();
        tripRepository.save(trip);

        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod(Purchase.PaymentMethod.CASH)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .build();
        purchaseRepository.save(purchase);

        Passenger passenger = Passenger.builder()
                .fullName("Jane Smith")
                .documentType("CC")
                .documentNumber("123456")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("3001234567")
                .createdAt(OffsetDateTime.now())
                .user(user)
                .build();
        passengerRepository.save(passenger);

        Ticket ticket = Ticket.builder()
                .trip(trip)
                .passenger(passenger)
                .purchase(purchase)
                .fromStop(fromStop)
                .toStop(toStop)
                .seatNumber("A1")
                .price(BigDecimal.valueOf(50000))
                .status(Ticket.Status.SOLD)
                .qrCode("QR-A1")
                .build();
        return ticketRepository.save(ticket);
    }

    @Test
    @DisplayName("Debe encontrar equipaje por ticket ID")
    void shouldFindBaggageByTicketId() {
        // Given
        Ticket ticket = givenTicket();
        Baggage baggage = Baggage.builder()
                .ticket(ticket)
                .weightKg(25.0)
                .fee(BigDecimal.valueOf(15000))
                .tagCode("TAG-001")
                .build();
        baggageRepository.save(baggage);

        // When
        List<Baggage> found = baggageRepository.findByTicketId(ticket.getId());

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getWeightKg()).isEqualTo(25.0);
        assertThat(found.get(0).getTagCode()).isEqualTo("TAG-001");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando ticket no tiene equipaje")
    void shouldReturnEmptyListWhenNoBaggage() {
        // Given
        Ticket ticket = givenTicket();

        // When
        List<Baggage> found = baggageRepository.findByTicketId(ticket.getId());

        // Then
        assertThat(found).isEmpty();
    }
    @Test
    @DisplayName("Debe contar equipajes por ticket ID")
    void shouldCountBaggageByTicketId() {
        // Given
        Ticket ticket = givenTicket();
        Baggage baggage1 = Baggage.builder()
                .ticket(ticket)
                .weightKg(25.0)
                .fee(BigDecimal.valueOf(15000))
                .tagCode("TAG-001")
                .build();
        Baggage baggage2 = Baggage.builder()
                .ticket(ticket)
                .weightKg(15.0)
                .fee(BigDecimal.valueOf(0))
                .tagCode("TAG-002")
                .build();
        baggageRepository.save(baggage1);
        baggageRepository.save(baggage2);

        // When
        int count = baggageRepository.countByTicketId(ticket.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe retornar 0 cuando ticket no tiene equipajes")
    void shouldReturnZeroWhenTicketHasNoBaggage() {
        // Given
        Ticket ticket = givenTicket();

        // When
        int count = baggageRepository.countByTicketId(ticket.getId());

        // Then
        assertThat(count).isEqualTo(0);
    }
}
