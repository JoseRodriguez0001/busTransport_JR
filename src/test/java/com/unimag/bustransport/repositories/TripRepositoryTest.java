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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;


class TripRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    // Datos de prueba reutilizables
    private Route route1;
    private Route route2;
    private Bus bus1;
    private Bus bus2;
    private Trip trip1;
    private Trip trip2;
    private Trip trip3;
    private Trip trip4;
    private Trip trip5;
    private LocalDate today;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        // Limpiar BD
        ticketRepository.deleteAll();
        tripRepository.deleteAll();
        stopRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();

        today = LocalDate.now();
        tomorrow = today.plusDays(1);

        // Crear rutas
        route1 = createRoute("R001", "Santa Marta", "Barranquilla", 100.0);
        route2 = createRoute("R002", "Cartagena", "Medellín", 500.0);

        // Crear buses
        bus1 = createBus("ABC123", 40);
        bus2 = createBus("XYZ789", 30);

        // Crear trips para hoy
        trip1 = createTrip(route1, bus1, today, "08:00", "10:00", Trip.Status.SCHEDULED);
        trip2 = createTrip(route1, bus1, today, "14:00", "16:00", Trip.Status.SCHEDULED);
        trip3 = createTrip(route1, bus2, today, "18:00", "20:00", Trip.Status.DEPARTED);

        // Crear trips para mañana
        trip4 = createTrip(route2, bus2, tomorrow, "09:00", "15:00", Trip.Status.SCHEDULED);

        // Crear trip cancelado
        trip5 = createTrip(route1, bus1, today, "20:00", "22:00", Trip.Status.CANCELLED);
    }

    @Test
    @DisplayName("Debe encontrar trips por ruta, fecha y estado")
    void findByRouteIdAndDateAndStatus_ShouldReturnMatchingTrips() {
        // When
        List<Trip> trips = tripRepository.findByRouteIdAndDateAndStatus(
                route1.getId(),
                today,
                Trip.Status.SCHEDULED
        );

        // Then
        assertThat(trips)
                .hasSize(2)
                .extracting(Trip::getId)
                .containsExactlyInAnyOrder(trip1.getId(), trip2.getId());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay trips con ese estado")
    void findByRouteIdAndDateAndStatus_ShouldReturnEmpty_WhenNoTripsWithStatus() {
        // When - Buscar trips ARRIVED (no hay ninguno)
        List<Trip> trips = tripRepository.findByRouteIdAndDateAndStatus(
                route1.getId(),
                today,
                Trip.Status.ARRIVED
        );

        // Then
        assertThat(trips).isEmpty();
    }


    @Test
    @DisplayName("Debe encontrar trips SCHEDULED por origen, destino y fecha")
    void findTripsByOriginAndDestination_ShouldReturnScheduledTrips() {
        // When
        List<Trip> trips = tripRepository.findTripsByOriginAndDestination(
                "Santa Marta",
                "Barranquilla",
                today
        );

        // Then
        assertThat(trips)
                .hasSize(2) // Solo SCHEDULED, no incluye DEPARTED ni CANCELLED
                .allMatch(trip -> trip.getStatus() == Trip.Status.SCHEDULED)
                .extracting(Trip::getId)
                .containsExactlyInAnyOrder(trip1.getId(), trip2.getId());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay trips para ese origen-destino")
    void findTripsByOriginAndDestination_ShouldReturnEmpty_WhenNoTrips() {
        // When - Buscar ruta que no existe
        List<Trip> trips = tripRepository.findTripsByOriginAndDestination(
                "Bogotá",
                "Cali",
                today
        );

        // Then
        assertThat(trips).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay trips en esa fecha")
    void findTripsByOriginAndDestination_ShouldReturnEmpty_WhenNoTripsOnDate() {
        // When - Buscar en una fecha sin trips
        LocalDate nextWeek = today.plusDays(7);
        List<Trip> trips = tripRepository.findTripsByOriginAndDestination(
                "Santa Marta",
                "Barranquilla",
                nextWeek
        );

        // Then
        assertThat(trips).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar todos los trips con estado SCHEDULED")
    void findByStatus_ShouldReturnAllScheduledTrips() {
        // When
        List<Trip> scheduledTrips = tripRepository.findByStatus(Trip.Status.SCHEDULED);

        // Then
        assertThat(scheduledTrips)
                .hasSize(3) // trip1, trip2, trip4
                .allMatch(trip -> trip.getStatus() == Trip.Status.SCHEDULED);
    }

    @Test
    @DisplayName("Debe encontrar trips DEPARTED")
    void findByStatus_ShouldReturnDepartedTrips() {
        // When
        List<Trip> departedTrips = tripRepository.findByStatus(Trip.Status.DEPARTED);

        // Then
        assertThat(departedTrips)
                .hasSize(1)
                .extracting(Trip::getId)
                .containsExactly(trip3.getId());
    }

    @Test
    @DisplayName("Debe encontrar trips de un bus específico con estado SCHEDULED")
    void findByBusIdAndStatus_ShouldReturnTripsByBusAndStatus() {
        // When
        List<Trip> trips = tripRepository.findByBusIdAndStatus(bus1.getId(), Trip.Status.SCHEDULED);

        // Then
        assertThat(trips)
                .hasSize(2) // trip1 y trip2 (trip5 está CANCELLED)
                .allMatch(trip -> trip.getBus().getId().equals(bus1.getId()))
                .allMatch(trip -> trip.getStatus() == Trip.Status.SCHEDULED);
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el bus no tiene trips en ese estado")
    void findByBusIdAndStatus_ShouldReturnEmpty_WhenNoTripsInStatus() {
        // When - bus2 no tiene trips CANCELLED
        List<Trip> trips = tripRepository.findByBusIdAndStatus(bus2.getId(), Trip.Status.CANCELLED);

        // Then
        assertThat(trips).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar trips próximos a salir (dentro del threshold)")
    void findTripsNearDeparture_ShouldReturnTripsNearDeparture() {
        // Given - Crear trip que sale en 30 minutos
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime departureIn30Min = now.plusMinutes(30);
        OffsetDateTime arrivalIn2Hours = departureIn30Min.plusHours(2);
        
        Trip nearTrip = createTrip(
                route1, 
                bus1, 
                today, 
                departureIn30Min, 
                arrivalIn2Hours, 
                Trip.Status.SCHEDULED
        );

        // When - Buscar trips que salen en los próximos 60 minutos
        OffsetDateTime threshold = now.plusMinutes(60);
        List<Trip> trips = tripRepository.findTripsNearDeparture(today, threshold);

        // Then
        assertThat(trips)
                .isNotEmpty()
                .anyMatch(trip -> trip.getId().equals(nearTrip.getId()));
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay trips próximos")
    void findTripsNearDeparture_ShouldReturnEmpty_WhenNoTripsNear() {
        // When - Buscar trips que salen en los próximos 5 minutos (no hay ninguno)
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime threshold = now.plusMinutes(5);
        List<Trip> trips = tripRepository.findTripsNearDeparture(today, threshold);

        // Then
        assertThat(trips).isEmpty();
    }

    @Test
    @DisplayName("Debe cargar trip con bus y seats usando fetch join")
    void findByIdWithBusAndSeats_ShouldLoadBusAndSeats() {
        // Given - Agregar seats al bus
        Seat seat1 = createSeat("1A", bus1);
        Seat seat2 = createSeat("1B", bus1);

        // When
        Optional<Trip> result = tripRepository.findByIdWithBusAndSeats(trip1.getId());

        // Then
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(trip -> {
                    assertThat(trip.getBus()).isNotNull();
                    assertThat(trip.getBus().getSeats())
                            .isNotNull()
                            .hasSize(2)
                            .extracting(Seat::getNumber)
                            .containsExactlyInAnyOrder("1A", "1B");
                });
    }

    @Test
    @DisplayName("Debe retornar vacío si el trip no existe")
    void findByIdWithBusAndSeats_ShouldReturnEmpty_WhenTripDoesNotExist() {
        // When
        Optional<Trip> result = tripRepository.findByIdWithBusAndSeats(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe contar correctamente los tickets vendidos de un trip")
    void countSoldTickets_ShouldReturnCorrectCount() {
        // Given - Crear tickets vendidos
        Passenger passenger = createPassenger("Jose", "3005543094", "CC", "12345678", today.plusDays(56));
        User user = createUser("user@test.com", "password", "3001234567");
        Purchase purchase = createPurchase(user, BigDecimal.valueOf(50000));
        Stop fromStop = createStop("Stop1", 0, route1);
        Stop toStop = createStop("Stop2", 1, route1);

        createTicket(trip1, passenger, "1A", BigDecimal.valueOf(25000), Ticket.Status.SOLD, purchase, fromStop, toStop);
        createTicket(trip1, passenger, "1B", BigDecimal.valueOf(25000), Ticket.Status.SOLD, purchase, fromStop, toStop);
        createTicket(trip1, passenger, "1C", BigDecimal.valueOf(25000), Ticket.Status.CANCELLED, purchase, fromStop, toStop);

        // When
        Long soldCount = tripRepository.countSoldTickets(trip1.getId());

        // Then
        assertThat(soldCount).isEqualTo(2); // Solo SOLD, no cuenta CANCELLED
    }

    @Test
    @DisplayName("Debe retornar 0 si el trip no tiene tickets vendidos")
    void countSoldTickets_ShouldReturnZero_WhenNoSoldTickets() {
        // When
        Long soldCount = tripRepository.countSoldTickets(trip2.getId());

        // Then
        assertThat(soldCount).isZero();
    }

    private Route createRoute(String code, String origin, String destination, Double distanceKm) {
        Route route = Route.builder()
                .code(code)
                .name(origin + " - " + destination)
                .origin(origin)
                .destination(destination)
                .distanceKm(distanceKm)
                .durationMin(120)
                .build();
        return routeRepository.save(route);
    }

    private Bus createBus(String plate, Integer capacity) {
        Bus bus = Bus.builder()
                .plate(plate)
                .capacity(capacity)
                .status(Bus.Status.ACTIVE)
                .amenities(List.of("WiFi", "AC"))
                .build();
        return busRepository.save(bus);
    }

    private Trip createTrip(Route route, Bus bus, LocalDate date, String departureTime, String arrivalTime, Trip.Status status) {
        OffsetDateTime departure = OffsetDateTime.of(date.atTime(
                Integer.parseInt(departureTime.split(":")[0]),
                Integer.parseInt(departureTime.split(":")[1])
        ), ZoneOffset.UTC);

        OffsetDateTime arrival = OffsetDateTime.of(date.atTime(
                Integer.parseInt(arrivalTime.split(":")[0]),
                Integer.parseInt(arrivalTime.split(":")[1])
        ), ZoneOffset.UTC);

        return createTrip(route, bus, date, departure, arrival, status);
    }

    private Trip createTrip(Route route, Bus bus, LocalDate date, OffsetDateTime departure, OffsetDateTime arrival, Trip.Status status) {
        Trip trip = Trip.builder()
                .route(route)
                .bus(bus)
                .date(date)
                .departureAt(departure)
                .arrivalAt(arrival)
                .status(status)
                .overbookingPercent(0.0)
                .build();
        return tripRepository.save(trip);
    }

    private Seat createSeat(String number, Bus bus) {
        Seat seat = Seat.builder()
                .number(number)
                .type(Seat.Type.STANDARD)
                .bus(bus)
                .build();
        return seat; // No guardamos aquí, se guarda con el bus
    }

    private Passenger createPassenger(String fullName, String phone, String documentType, String documentNumber, LocalDate birthDate) {
        Passenger passenger = Passenger.builder()
                .fullName(fullName)
                .phoneNumber(phone)
                .documentType(documentType)
                .documentNumber(documentNumber)
                .birthDate(birthDate)
                .build();
        return passengerRepository.save(passenger);
    }

    private User createUser(String email, String password, String phone) {
        User user = User.builder()
                .email(email)
                .passwordHash(password)
                .phone(phone)
                .role(Role.ROLE_PASSENGER)
                .build();
        return userRepository.save(user);
    }

    private Purchase createPurchase(User user, BigDecimal totalAmount) {
        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(totalAmount)
                .paymentMethod(Purchase.PaymentMethod.CARD)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .build();
        return purchaseRepository.save(purchase);
    }

    private Stop createStop(String name, Integer order, Route route) {
        Stop stop = Stop.builder()
                .name(name)
                .order(order)
                .lat(10.0)
                .lng(-75.0)
                .route(route)
                .build();
        return stopRepository.save(stop);
    }

    private Ticket createTicket(Trip trip, Passenger passenger, String seatNumber, BigDecimal price, 
                                Ticket.Status status, Purchase purchase, Stop fromStop, Stop toStop) {
        Ticket ticket = Ticket.builder()
                .trip(trip)
                .passenger(passenger)
                .seatNumber(seatNumber)
                .price(price)
                .status(status)
                .purchase(purchase)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        return ticketRepository.save(ticket);
    }
}