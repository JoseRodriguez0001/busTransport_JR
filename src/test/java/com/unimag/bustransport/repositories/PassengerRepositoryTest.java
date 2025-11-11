package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Passenger;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.PassengerRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class PassengerRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        passengerRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User givenUser() {
        return givenUser("test@example.com", "John Doe", Role.ROLE_PASSENGER);
    }

    private User givenUser(String email, String name, Role role) {
        User user = User.builder()
                .email(email)
                .name(name)
                .phone("+573001234567")
                .passwordHash("hashedPassword123")
                .role(role)
                .status(User.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private Passenger buildPassenger() {
        return buildPassenger("Juan Pérez", "CC", "123456789", null);
    }

    private Passenger buildPassenger(String fullName, String docType, String docNumber, User user) {
        return Passenger.builder()
                .fullName(fullName)
                .documentType(docType)
                .documentNumber(docNumber)
                .birthDate(LocalDate.of(1990, 5, 15))
                .phoneNumber("+573001234567")
                .createAt(OffsetDateTime.now())
                .user(user)
                .build();
    }

    private Passenger givenPassenger() {
        return passengerRepository.save(buildPassenger());
    }

    private Passenger givenPassengerWithUser(User user) {
        Passenger passenger = buildPassenger("María López", "CC", "987654321", user);
        return passengerRepository.save(passenger);
    }

    private Passenger givenPassengerWithDocument(String documentNumber) {
        Passenger passenger = buildPassenger("Carlos Ruiz", "CC", documentNumber, null);
        return passengerRepository.save(passenger);
    }

    private List<Passenger> givenMultiplePassengersForUser(User user, int count) {
        List<Passenger> passengers = List.of(
            buildPassenger("Pasajero 1", "CC", "111111111", user),
            buildPassenger("Pasajero 2", "CC", "222222222", user),
            buildPassenger("Pasajero 3", "TI", "333333333", user),
            buildPassenger("Pasajero 4", "CC", "444444444", user),
            buildPassenger("Pasajero 5", "TI", "555555555", user)
        );
        
        return passengerRepository.saveAll(passengers.subList(0, Math.min(count, passengers.size())));
    }

    @Test
    @DisplayName("Debe guardar un pasajero correctamente")
    void shouldSavePassenger() {
        // Given
        Passenger passenger = buildPassenger();

        // When
        Passenger saved = passengerRepository.save(passenger);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFullName()).isEqualTo("Juan Pérez");
        assertThat(saved.getDocumentNumber()).isEqualTo("123456789");
        assertThat(saved.getDocumentType()).isEqualTo("CC");
    }

    @Test
    @DisplayName("Debe encontrar pasajeros por userId")
    void shouldFindPassengersByUserId() {
        // Given
        User user = givenUser();
        List<Passenger> passengers = givenMultiplePassengersForUser(user, 3);

        // When
        List<Passenger> found = passengerRepository.findByUserId(user.getId());

        // Then
        assertThat(found).hasSize(3);
        assertThat(found).extracting(Passenger::getFullName)
                .containsExactlyInAnyOrder("Pasajero 1", "Pasajero 2", "Pasajero 3");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando el usuario no tiene pasajeros")
    void shouldReturnEmptyListWhenUserHasNoPassengers() {
        // Given
        User user = givenUser();

        // When
        List<Passenger> found = passengerRepository.findByUserId(user.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando el userId no existe")
    void shouldReturnEmptyListWhenUserIdNotExists() {
        // When
        List<Passenger> found = passengerRepository.findByUserId(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("No debe retornar pasajeros de otros usuarios")
    void shouldNotReturnPassengersFromOtherUsers() {
        // Given
        User user1 = givenUser("user1@test.com", "User 1", Role.ROLE_PASSENGER);
        User user2 = givenUser("user2@test.com", "User 2", Role.ROLE_PASSENGER);
        
        givenMultiplePassengersForUser(user1, 2);
        givenMultiplePassengersForUser(user2, 3);

        // When
        List<Passenger> foundUser1 = passengerRepository.findByUserId(user1.getId());
        List<Passenger> foundUser2 = passengerRepository.findByUserId(user2.getId());

        // Then
        assertThat(foundUser1).hasSize(2);
        assertThat(foundUser2).hasSize(3);
        assertThat(foundUser1).allMatch(p -> p.getUser().getId().equals(user1.getId()));
        assertThat(foundUser2).allMatch(p -> p.getUser().getId().equals(user2.getId()));
    }

    @Test
    @DisplayName("Debe encontrar pasajero por número de documento")
    void shouldFindPassengerByDocumentNumber() {
        // Given
        Passenger saved = givenPassengerWithDocument("123456789");

        // When
        Optional<Passenger> found = passengerRepository.findByDocumentNumber("123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getDocumentNumber()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("Debe retornar empty cuando el documento no existe")
    void shouldReturnEmptyWhenDocumentNumberNotFound() {
        // When
        Optional<Passenger> found = passengerRepository.findByDocumentNumber("999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar true cuando el documento existe")
    void shouldReturnTrueWhenDocumentExists() {
        // Given
        givenPassengerWithDocument("123456789");

        // When
        boolean exists = passengerRepository.existsByDocumentNumber("123456789");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false cuando el documento no existe")
    void shouldReturnFalseWhenDocumentNotExists() {
        // When
        boolean exists = passengerRepository.existsByDocumentNumber("999999999");

        // Then
        assertThat(exists).isFalse();
    }

    // ==================== TESTS EDGE CASES ====================

    @Test
    @DisplayName("Debe permitir múltiples pasajeros con el mismo número de documento")
    void shouldAllowMultiplePassengersWithSameDocument() {
        // Given
        Passenger passenger1 = buildPassenger("Juan Pérez", "CC", "123456789", null);
        Passenger passenger2 = buildPassenger("Juan Pérez Gómez", "CC", "123456789", null);

        // When
        Passenger saved1 = passengerRepository.save(passenger1);
        Passenger saved2 = passengerRepository.save(passenger2);

        // Then
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(saved1.getDocumentNumber()).isEqualTo(saved2.getDocumentNumber());
    }

    @Test
    @DisplayName("Debe guardar pasajero con todos los campos opcionales en null")
    void shouldSavePassengerWithNullOptionalFields() {
        // Given
        Passenger passenger = Passenger.builder()
                .fullName("Minimal Passenger")
                .documentType(null)
                .documentNumber(null)
                .birthDate(null)
                .phoneNumber(null)
                .createAt(OffsetDateTime.now())
                .user(null)
                .build();

        // When
        Passenger saved = passengerRepository.save(passenger);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFullName()).isEqualTo("Minimal Passenger");
        assertThat(saved.getDocumentType()).isNull();
        assertThat(saved.getDocumentNumber()).isNull();
        assertThat(saved.getBirthDate()).isNull();
        assertThat(saved.getPhoneNumber()).isNull();
        assertThat(saved.getUser()).isNull();
    }
}
