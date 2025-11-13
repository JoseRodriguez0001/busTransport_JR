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

public class SeatHoldRepositoryTest extends AbstractRepositoryTI{

    @Autowired
    private SeatHoldRepository seatHoldRepository;

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
        seatHoldRepository.deleteAll();
        tripRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User givenUser(String email) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .passwordHash("hash")
                .role(Role.ROLE_PASSENGER)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
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

    @Test
    @DisplayName("Debe encontrar seat holds por user ID")
    void shouldFindSeatHoldsByUserId() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();

        SeatHold hold1 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("A1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold1);

        SeatHold hold2 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("A2")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold2);

        // When
        List<SeatHold> found = seatHoldRepository.findByUserId(user.getId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(SeatHold::getSeatNumber)
                .containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    @DisplayName("Debe encontrar seat holds activos por trip y usuario")
    void shouldFindActiveSeatHoldsByTripAndUser() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();
        OffsetDateTime now = OffsetDateTime.now();

        SeatHold activeHold = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("B1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.plusMinutes(10))
                .build();
        seatHoldRepository.save(activeHold);

        SeatHold expiredHold = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("B2")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.minusMinutes(5))
                .build();
        seatHoldRepository.save(expiredHold);

        // When
        List<SeatHold> found = seatHoldRepository.findByTripIdAndUserIdAndStatusAndExpiresAtAfter(
                trip.getId(),
                user.getId(),
                SeatHold.Status.HOLD,
                now
        );

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSeatNumber()).isEqualTo("B1");
    }

    @Test
    @DisplayName("Debe encontrar seat holds activos por trip")
    void shouldFindActiveSeatHoldsByTrip() {
        // Given
        User user1 = givenUser("user1@example.com");
        User user2 = givenUser("user2@example.com");
        Trip trip = givenTrip();
        OffsetDateTime now = OffsetDateTime.now();

        SeatHold activeHold1 = SeatHold.builder()
                .trip(trip)
                .user(user1)
                .seatNumber("G1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.plusMinutes(10))
                .build();
        seatHoldRepository.save(activeHold1);

        SeatHold activeHold2 = SeatHold.builder()
                .trip(trip)
                .user(user2)
                .seatNumber("G2")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.plusMinutes(5))
                .build();
        seatHoldRepository.save(activeHold2);

        SeatHold expiredHold = SeatHold.builder()
                .trip(trip)
                .user(user1)
                .seatNumber("G3")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.minusMinutes(2))
                .build();
        seatHoldRepository.save(expiredHold);

        // When
        List<SeatHold> found = seatHoldRepository.findByTripIdAndStatusAndExpiresAtAfter(
                trip.getId(),
                SeatHold.Status.HOLD,
                now
        );

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(SeatHold::getSeatNumber)
                .containsExactlyInAnyOrder("G1", "G2");
    }

    @Test
    @DisplayName("Debe encontrar seat holds por trip y asientos específicos")
    void shouldFindSeatHoldsByTripAndSeatNumbers() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();

        SeatHold hold1 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("C1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold1);

        SeatHold hold2 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("C2")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold2);

        SeatHold hold3 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("C3")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold3);

        // When
        List<SeatHold> found = seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                trip.getId(),
                List.of("C1", "C3"),
                SeatHold.Status.HOLD
        );

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(SeatHold::getSeatNumber)
                .containsExactlyInAnyOrder("C1", "C3");
    }

    @Test
    @DisplayName("Debe verificar si existe seat hold activo")
    void shouldCheckIfActiveSeatHoldExists() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();
        OffsetDateTime now = OffsetDateTime.now();

        SeatHold hold = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("D1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(now.plusMinutes(10))
                .build();
        seatHoldRepository.save(hold);

        // When
        boolean exists = seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                trip.getId(),
                "D1",
                SeatHold.Status.HOLD,
                now
        );
        boolean notExists = seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                trip.getId(),
                "D2",
                SeatHold.Status.HOLD,
                now
        );

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Debe encontrar seat holds por status")
    void shouldFindSeatHoldsByStatus() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();

        SeatHold hold1 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("E1")
                .status(SeatHold.Status.EXPIRED)
                .expiresAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        seatHoldRepository.save(hold1);

        SeatHold hold2 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("E2")
                .status(SeatHold.Status.EXPIRED)
                .expiresAt(OffsetDateTime.now().minusMinutes(3))
                .build();
        seatHoldRepository.save(hold2);

        SeatHold hold3 = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("E3")
                .status(SeatHold.Status.HOLD)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        seatHoldRepository.save(hold3);

        // When
        List<SeatHold> found = seatHoldRepository.findByStatus(SeatHold.Status.EXPIRED);

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(h -> h.getStatus() == SeatHold.Status.EXPIRED);
    }

    @Test
    @DisplayName("Debe encontrar seat holds expirados antes de una fecha")
    void shouldFindExpiredSeatHoldsBeforeDate() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();
        OffsetDateTime cutoffDate = OffsetDateTime.now();

        SeatHold oldExpired = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("F1")
                .status(SeatHold.Status.HOLD)
                .expiresAt(cutoffDate.minusMinutes(10))
                .build();
        seatHoldRepository.save(oldExpired);

        SeatHold recentExpired = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("F2")
                .status(SeatHold.Status.HOLD)
                .expiresAt(cutoffDate.minusMinutes(2))
                .build();
        seatHoldRepository.save(recentExpired);

        SeatHold notExpired = SeatHold.builder()
                .trip(trip)
                .user(user)
                .seatNumber("F3")
                .status(SeatHold.Status.HOLD)
                .expiresAt(cutoffDate.plusMinutes(10))
                .build();
        seatHoldRepository.save(notExpired);

        // When
        List<SeatHold> found = seatHoldRepository.findByStatusAndExpiresAtBefore(
                SeatHold.Status.HOLD,
                cutoffDate
        );

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(SeatHold::getSeatNumber)
                .containsExactlyInAnyOrder("F1", "F2");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando usuario no tiene seat holds")
    void shouldReturnEmptyListWhenUserHasNoSeatHolds() {
        // Given
        User user = givenUser("user@example.com");

        // When
        List<SeatHold> found = seatHoldRepository.findByUserId(user.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar empty cuando no encuentra seat hold específico")
    void shouldReturnEmptyWhenSeatHoldNotFound() {
        // Given
        User user = givenUser("user@example.com");
        Trip trip = givenTrip();

        // When
        Optional<SeatHold> found = seatHoldRepository.findByTripIdAndSeatNumberAndUserId(
                trip.getId(),
                "Z99",
                user.getId()
        );

        // Then
        assertThat(found).isEmpty();
    }
}
