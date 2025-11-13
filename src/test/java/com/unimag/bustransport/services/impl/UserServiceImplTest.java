package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.InvalidCredentialsException;
import com.unimag.bustransport.services.UserService;
import com.unimag.bustransport.services.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, mapper);
    }

    private User givenUser(Long id, String email, Role role, User.Status status) {
        return User.builder()
                .id(id)
                .email(email)
                .name("Test User")
                .phone("3001234567")
                .passwordHash("password123")
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private UserDtos.UserCreateRequest givenRegisterRequest() {
        return new UserDtos.UserCreateRequest(
                "test@example.com",
                "TestUser",
                "Jose Rodriguez",
                "3005540394"
        );
    }

    private UserDtos.EmployeeCreateRequest givenEmployeeRequest(Role role) {
        return new UserDtos.EmployeeCreateRequest(
                "employee@test.com",
                "Employee Name",
                "3009876543",
                role
        );
    }

    @Test
    @DisplayName("Debe registrar un pasajero correctamente")
    void shouldRegisterPassenger(){
        //given
        UserDtos.UserCreateRequest request = givenRegisterRequest();
        User userSaved = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.findByPhone("3001234567")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(userSaved);

        //when

        UserDtos.UserResponse response = userService.registerUser(request);

        //Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_PASSENGER);

        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test

    @DisplayName("Debe crear un empleado con rol DRIVER")
    void shouldRegisterEmployee(){
        //given
        UserDtos.EmployeeCreateRequest request = givenEmployeeRequest(Role.ROLE_DRIVER);
        User userSaved = givenUser(1L,"employee@gmail.com", Role.ROLE_DRIVER, User.Status.ACTIVE);

        when(userRepository.existsByEmail("employee@gmail.com")).thenReturn(false);
        when(userRepository.findByPhone("3009876543")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(userSaved);

        //when

        UserDtos.UserResponse response = userService.createEmployee(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(Role.ROLE_DRIVER);

        verify(userRepository,times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al crear empleado con rol PASSENGER")
    void shouldThrowExceptionWhenCreatingPassengerAsEmployee() {
        // Given
        UserDtos.EmployeeCreateRequest request = givenEmployeeRequest(Role.ROLE_PASSENGER);

        // When & Then
        assertThatThrownBy(() -> userService.createEmployee(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create PASSENGER accounts with this method");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debe hacer login correctamente")
    void shouldLoginSuccessfully() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        UserDtos.UserResponse response = userService.login("test@example.com", "password123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el email no existe")
    void shouldThrowExceptionWhenEmailNotFoundOnLogin() {
        // Given
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login("notfound@test.com", "password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository, times(1)).findByEmail("notfound@test.com");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la contraseña es incorrecta")
    void shouldThrowExceptionWhenPasswordIncorrect() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.login("test@example.com", "wrongpassword"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario está inactivo")
    void shouldThrowExceptionWhenUserInactive() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.INACTIVE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.login("test@example.com", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("User account is INACTIVE");
    }

    @Test
    @DisplayName("Debe actualizar nombre y teléfono del usuario")
    void shouldUpdateUserNameAndPhone() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);
        UserDtos.UserUpdateRequest request = new UserDtos.UserUpdateRequest(
                "Updated Name",
                "3009999999"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByPhone("3009999999")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.updateUser(1L, request);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Debe cambiar la contraseña correctamente")
    void shouldChangePassword() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.changePassword(1L, "password123", "NewPassword123");

        // Then
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Debe desactivar un usuario correctamente")
    void shouldDeactivateUser() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.desactivateUser(1L);

        // Then
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Debe reactivar un usuario correctamente")
    void shouldReactivateUser() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.INACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.reactivateUser(1L);

        // Then
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Debe obtener usuario por ID")
    void shouldGetUserById() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        UserDtos.UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe obtener usuario por email")
    void shouldGetUserByEmail() {
        // Given
        User user = givenUser(1L, "test@example.com", Role.ROLE_PASSENGER, User.Status.ACTIVE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        UserDtos.UserResponse response = userService.getUserByEmail("test@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Debe obtener usuarios por rol")
    void shouldGetUsersByRole() {
        // Given
        List<User> users = List.of(
                givenUser(1L, "driver1@test.com", Role.ROLE_DRIVER, User.Status.ACTIVE),
                givenUser(2L, "driver2@test.com", Role.ROLE_DRIVER, User.Status.ACTIVE)
        );

        when(userRepository.findByRoleAndStatus(Role.ROLE_DRIVER, User.Status.ACTIVE)).thenReturn(users);

        // When
        List<UserDtos.UserResponse> result = userService.getAllUsersByRole(Role.ROLE_DRIVER);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.role() == Role.ROLE_DRIVER);
    }

}
