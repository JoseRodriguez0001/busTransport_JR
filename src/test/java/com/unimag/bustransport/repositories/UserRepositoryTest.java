package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class UserRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User givenUser(String email, String name, Role role, User.Status status) {
        User user = User.builder()
                .email(email)
                .name(name)
                .phone("+573001234567")
                .passwordHash("hashedPassword123")
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }
    private User givenUser(String email, String name, Role role, User.Status status,String phone) {
        User user = User.builder()
                .email(email)
                .name(name)
                .phone(phone)
                .passwordHash("hashedPassword123")
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Debe encontrar usuario por email")
    void shouldFindUserByEmail() {
        // Given
        User saved = givenUser("test@example.com", "Test User", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Debe retornar empty cuando el email no existe")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("noexiste@test.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar usuario por teléfono")
    void shouldFindUserByPhone() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .phone("+573009999999")
                .passwordHash("hash")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        User saved = userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByPhone("+573009999999");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Debe retornar empty cuando el teléfono no existe")
    void shouldReturnEmptyWhenPhoneNotFound() {
        // When
        Optional<User> found = userRepository.findByPhone("+573009999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar true cuando el email existe")
    void shouldReturnTrueWhenEmailExists() {
        // Given
        givenUser("exists@test.com", "User", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        // When
        boolean exists = userRepository.existsByEmail("exists@test.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false cuando el email no existe")
    void shouldReturnFalseWhenEmailNotExists() {
        // When
        boolean exists = userRepository.existsByEmail("noexiste@test.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Debe encontrar usuarios por rol y status")
    void shouldFindUsersByRoleAndStatus() {
        // Given
        givenUser("driver1@test.com", "Driver 1", Role.ROLE_DRIVER, User.Status.ACTIVE);
        givenUser("driver2@test.com", "Driver 2", Role.ROLE_DRIVER, User.Status.ACTIVE,"3005645647");
        givenUser("driver3@test.com", "Driver 3", Role.ROLE_DRIVER, User.Status.INACTIVE,"3224343434");
        givenUser("passenger@test.com", "Passenger", Role.ROLE_PASSENGER, User.Status.ACTIVE,"3213455654");

        // When
        List<User> activeDrivers = userRepository.findByRoleAndStatus(Role.ROLE_DRIVER,User.Status.ACTIVE);

        // Then
        assertThat(activeDrivers).hasSize(2);
        assertThat(activeDrivers).allMatch(u -> u.getRole() == Role.ROLE_DRIVER);
        assertThat(activeDrivers).allMatch(u -> u.getStatus() == User.Status.ACTIVE);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay usuarios con rol y status")
    void shouldReturnEmptyListWhenNoUsersWithRoleAndStatus() {
        // Given
        givenUser("driver@test.com", "Driver", Role.ROLE_DRIVER, User.Status.ACTIVE);

        // When
        List<User> dispatchers = userRepository.findByRoleAndStatus(Role.ROLE_DISPATCHER, User.Status.ACTIVE);

        // Then
        assertThat(dispatchers).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar usuarios activos por rol usando query JPQL")
    void shouldFindActiveUsersByRole() {
        // Given
        givenUser("driver1@test.com", "Driver 1", Role.ROLE_DRIVER, User.Status.ACTIVE);
        givenUser("driver2@test.com", "Driver 2", Role.ROLE_DRIVER, User.Status.ACTIVE,"3005645647");
        givenUser("driver3@test.com", "Driver 3", Role.ROLE_DRIVER, User.Status.INACTIVE,"3213455654");
        givenUser("driver4@test.com", "Driver 4", Role.ROLE_DRIVER, User.Status.BLOCKED,"3023455667");

        // When
        List<User> activeDrivers = userRepository.findByRoleAndStatus(Role.ROLE_DRIVER,User.Status.ACTIVE);

        // Then
        assertThat(activeDrivers).hasSize(2);
        assertThat(activeDrivers).allMatch(u -> u.getRole() == Role.ROLE_DRIVER);
        assertThat(activeDrivers).allMatch(u -> u.getStatus() == User.Status.ACTIVE);
        assertThat(activeDrivers).extracting(User::getName)
                .containsExactlyInAnyOrder("Driver 1", "Driver 2");
    }

    @Test
    @DisplayName("Debe retornar solo usuarios ACTIVE, no INACTIVE ni BLOCKED")
    void shouldReturnOnlyActiveUsersNotInactiveOrBlocked() {
        // Given
        givenUser("pass1@test.com", "Pass Active", Role.ROLE_PASSENGER, User.Status.ACTIVE);
        givenUser("pass2@test.com", "Pass Inactive", Role.ROLE_PASSENGER, User.Status.INACTIVE,"3423455667");
        givenUser("pass3@test.com", "Pass Blocked", Role.ROLE_PASSENGER, User.Status.BLOCKED,"3545677889");

        // When
        List<User> activePassengers = userRepository.findByRoleAndStatus(Role.ROLE_PASSENGER, User.Status.ACTIVE);

        // Then
        assertThat(activePassengers).hasSize(1);
        assertThat(activePassengers.get(0).getName()).isEqualTo("Pass Active");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay usuarios activos del rol")
    void shouldReturnEmptyListWhenNoActiveUsersOfRole() {
        // Given
        givenUser("driver@test.com", "Driver", Role.ROLE_DRIVER, User.Status.INACTIVE,"3213455654");

        // When
        List<User> activeDrivers = userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, User.Status.ACTIVE);

        // Then
        assertThat(activeDrivers).isEmpty();
    }
}
