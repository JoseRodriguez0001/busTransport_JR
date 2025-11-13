package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignmentRepositoryTest extends AbstractRepositoryTI{

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusRepository busRepository;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        tripRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Trip givenTrip() {
        Route route = Route.builder()
                .code("R001")
                .origin("A")
                .destination("B")
                .distanceKm(100.0)
                .durationMin(120)
                .build();
        routeRepository.save(route);

        Bus bus = Bus.builder()
                .plate("ABC123")
                .capacity(40)
                .status(Bus.Status.ACTIVE)
                .build();
        busRepository.save(bus);

        Trip trip = Trip.builder()
                .route(route)
                .bus(bus)
                .date(LocalDate.now())
                .departureAt(OffsetDateTime.now())
                .arrivalAt(OffsetDateTime.now().plusHours(2))
                .build();
        return tripRepository.save(trip);
    }

    private User givenUser(String email, Role role) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .passwordHash("hash")
                .role(role)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Debe encontrar assignment por trip ID")
    void shouldFindAssignmentByTripId() {
        // Given
        User driver = givenUser("driver@example.com", Role.ROLE_DRIVER);
        User dispatcher = givenUser("dispatcher@example.com", Role.ROLE_DISPATCHER);
        Trip trip = givenTrip();

        Assignment assignment = Assignment.builder()
                .trip(trip)
                .driver(driver)
                .dispatcher(dispatcher)
                .assignedAt(OffsetDateTime.now())
                .build();
        assignmentRepository.save(assignment);

        // When
        Optional<Assignment> found = assignmentRepository.findByTripId(trip.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDriver().getId()).isEqualTo(driver.getId());
    }

    @Test
    @DisplayName("Debe encontrar assignments por driver ID")
    void shouldFindAssignmentsByDriver() {
        // Given
        User driver = givenUser("driver@example.com", Role.ROLE_DRIVER);
        User dispatcher = givenUser("dispatcher@example.com", Role.ROLE_DISPATCHER);
        Trip trip1 = givenTrip();
        Trip trip2 = givenTrip();

        Assignment assignment1 = Assignment.builder()
                .trip(trip1)
                .driver(driver)
                .dispatcher(dispatcher)
                .assignedAt(OffsetDateTime.now())
                .build();
        assignmentRepository.save(assignment1);

        Assignment assignment2 = Assignment.builder()
                .trip(trip2)
                .driver(driver)
                .dispatcher(dispatcher)
                .assignedAt(OffsetDateTime.now())
                .build();
        assignmentRepository.save(assignment2);

        // When
        List<Assignment> found = assignmentRepository.findByDriver(driver.getId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(a -> a.getDriver().getId().equals(driver.getId()));
    }
}
