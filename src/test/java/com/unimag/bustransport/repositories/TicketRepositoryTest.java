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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TicketRepository Integration Tests")
public class TicketRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private PassengerRepository passengerRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        purchaseRepository.deleteAll();
        passengerRepository.deleteAll();
        tripRepository.deleteAll();
        stopRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User givenUser() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .phone("+573001234567")
                .passwordHash("hash")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private Bus givenBus() {
        Bus bus = Bus.builder()
                .plate("ABC123")
                .capacity(40)
                .amenities(new ArrayList<>())
                .status(Bus.Status.ACTIVE)
                .build();
        return busRepository.save(bus);
    }

    private Route givenRoute() {
        Route route = Route.builder().code("RUT567")
                .name("BOG-MED")
                .origin("Bogotá")
                .destination("Medellín")
                .durationMin(360)
                .distanceKm(415.5)
                .build();
        return routeRepository.save(route);
    }

    private Stop givenStop(Route route, String name, Integer order) {
        Stop stop = Stop.builder()
                .name(name)
                .order(order)
                .lat(4.6097)
                .lng(-74.0817)
                .route(route)
                .build();
        return stopRepository.save(stop);
    }

    private Trip givenTrip(Bus bus, Route route) {
        Trip trip = Trip.builder()
                .date(LocalDate.now().plusDays(1))  // ← AGREGAR ESTO
                .bus(bus)
                .route(route)
                .departureAt(OffsetDateTime.now().plusDays(1))
                .arrivalAt(OffsetDateTime.now().plusDays(1).plusHours(6))
                .overbookingPercent(0.0)
                .status(Trip.Status.SCHEDULED)
                .build();
        return tripRepository.save(trip);
    }

    private Passenger givenPassenger() {
        Passenger passenger = Passenger.builder()
                .fullName("Juan Pérez")
                .documentType("CC")
                .documentNumber("123456789")
                .birthDate(LocalDate.of(1990, 5, 15))
                .phoneNumber("+573001234567")
                .createdAt(OffsetDateTime.now())
                .build();
        return passengerRepository.save(passenger);
    }

    private Purchase givenPurchase(User user) {
        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod(Purchase.PaymentMethod.CARD)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .build();
        return purchaseRepository.save(purchase);
    }

    private Ticket givenTicket(Trip trip, Passenger passenger, Purchase purchase, 
                               Stop fromStop, Stop toStop, String seatNumber, Ticket.Status status) {
        Ticket ticket = Ticket.builder()
                .seatNumber(seatNumber)
                .price(BigDecimal.valueOf(50000))
                .status(status)
                .qrCode("TICKET-" + seatNumber)
                .trip(trip)
                .passenger(passenger)
                .fromStop(fromStop)
                .toStop(toStop)
                .purchase(purchase)
                .build();
        return ticketRepository.save(ticket);
    }

    @Test
    @DisplayName("Debe encontrar tickets por tripId")
    void shouldFindTicketsByTripId() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal Bogotá", 1);
        Stop stop2 = givenStop(route, "Terminal Medellín", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1B", Ticket.Status.SOLD);

        // When
        List<Ticket> tickets = ticketRepository.findByTripId(trip.getId());

        // Then
        assertThat(tickets).hasSize(2);
        assertThat(tickets).allMatch(t -> t.getTrip().getId().equals(trip.getId()));
    }

    @Test
    @DisplayName("Debe encontrar tickets por passengerId")
    void shouldFindTicketsByPassengerId() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "2A", Ticket.Status.SOLD);

        // When
        List<Ticket> tickets = ticketRepository.findByPassengerId(passenger.getId());

        // Then
        assertThat(tickets).hasSize(2);
        assertThat(tickets).allMatch(t -> t.getPassenger().getId().equals(passenger.getId()));
    }

    @Test
    @DisplayName("Debe encontrar tickets por purchaseId")
    void shouldFindTicketsByPurchaseId() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1B", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1C", Ticket.Status.SOLD);

        // When
        List<Ticket> tickets = ticketRepository.findByPurchaseId(purchase.getId());

        // Then
        assertThat(tickets).hasSize(3);
        assertThat(tickets).allMatch(t -> t.getPurchase().getId().equals(purchase.getId()));
    }

    @Test
    @DisplayName("Debe encontrar ticket por QR code")
    void shouldFindTicketByQrCode() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        Ticket saved = givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);

        // When
        Optional<Ticket> found = ticketRepository.findByQrCode("TICKET-1A");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Debe retornar empty cuando QR code no existe")
    void shouldReturnEmptyWhenQrCodeNotFound() {
        // When
        Optional<Ticket> found = ticketRepository.findByQrCode("NOEXISTE");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar tickets por status")
    void shouldFindTicketsByStatus() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1B", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1C", Ticket.Status.CANCELLED);

        // When
        List<Ticket> soldTickets = ticketRepository.findByStatus(Ticket.Status.SOLD);

        // Then
        assertThat(soldTickets).hasSize(2);
        assertThat(soldTickets).allMatch(t -> t.getStatus() == Ticket.Status.SOLD);
    }

    @Test
    @DisplayName("Debe contar tickets vendidos por trip")
    void shouldCountSoldByTrip() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1B", Ticket.Status.SOLD);
        givenTicket(trip, passenger, purchase, stop1, stop2, "1C", Ticket.Status.CANCELLED);

        // When
        long count = ticketRepository.countSoldByTrip(trip.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Debe verificar si existe ticket vendido por trip y asiento")
    void shouldCheckIfTicketExistsByTripAndSeatAndStatus() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Terminal", 1);
        Stop stop2 = givenStop(route, "Destino", 2);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        givenTicket(trip, passenger, purchase, stop1, stop2, "5A", Ticket.Status.SOLD);

        // When
        boolean exists = ticketRepository.existsByTripIdAndSeatNumberAndStatus(
            trip.getId(), "5A", Ticket.Status.SOLD
        );
        boolean notExists = ticketRepository.existsByTripIdAndSeatNumberAndStatus(
            trip.getId(), "5B", Ticket.Status.SOLD
        );

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Debe encontrar tickets con solapamiento de tramos")
    void shouldFindOverlappingTickets() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Parada 1", 1);
        Stop stop2 = givenStop(route, "Parada 2", 2);
        Stop stop3 = givenStop(route, "Parada 3", 3);
        Stop stop4 = givenStop(route, "Parada 4", 4);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        // Ticket existente: asiento 1A desde parada 1 (order=1) hasta parada 3 (order=3)
        givenTicket(trip, passenger, purchase, stop1, stop3, "1A", Ticket.Status.SOLD);

        // When - Buscar solapamiento para asiento 1A desde parada 2 hasta parada 4
        List<Ticket> overlapping = ticketRepository.findOverlappingTickets(
            trip.getId(),
            "1A",
            2,  // fromOrder
            4   // toOrder
        );

        // Then
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.get(0).getSeatNumber()).isEqualTo("1A");
    }

    @Test
    @DisplayName("No debe encontrar solapamiento cuando los tramos no se cruzan")
    void shouldNotFindOverlappingWhenSegmentsDoNotOverlap() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Parada 1", 1);
        Stop stop2 = givenStop(route, "Parada 2", 2);
        Stop stop3 = givenStop(route, "Parada 3", 3);
        Stop stop4 = givenStop(route, "Parada 4", 4);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        // Ticket existente: asiento 1A desde parada 1 hasta parada 2
        givenTicket(trip, passenger, purchase, stop1, stop2, "1A", Ticket.Status.SOLD);

        // When - Buscar solapamiento para asiento 1A desde parada 3 hasta parada 4 (NO se solapa)
        List<Ticket> overlapping = ticketRepository.findOverlappingTickets(
            trip.getId(),
            "1A",
            3,  // fromOrder
            4   // toOrder
        );

        // Then
        assertThat(overlapping).isEmpty();
    }

    @Test
    @DisplayName("Debe contar asientos ocupados entre paradas")
    void shouldCountSeatOccupiedBetweenStops() {
        // Given
        User user = givenUser();
        Bus bus = givenBus();
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Parada 1", 1);
        Stop stop2 = givenStop(route, "Parada 2", 2);
        Stop stop3 = givenStop(route, "Parada 3", 3);
        Trip trip = givenTrip(bus, route);
        Passenger passenger = givenPassenger();
        Purchase purchase = givenPurchase(user);
        
        // Ticket: asiento 5A desde parada 1 hasta parada 3
        givenTicket(trip, passenger, purchase, stop1, stop3, "5A", Ticket.Status.SOLD);

        // When - Verificar ocupación del asiento 5A entre paradas 1 y 2
        long count = ticketRepository.countSeatOcuppyBetweenStops(
            trip.getId(),
            1,  // fromIndex
            2,  // toIndex
            "5A"
        );

        // Then
        assertThat(count).isEqualTo(1);
    }
}
