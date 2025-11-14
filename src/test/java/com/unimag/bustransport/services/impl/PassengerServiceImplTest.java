package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.PassengerRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.impl.PassengerServiceImpl;
import com.unimag.bustransport.services.mapper.PassengerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private final PassengerMapper passengerMapper = Mappers.getMapper(PassengerMapper.class);

    @InjectMocks
    private PassengerServiceImpl passengerService;


    private User givenUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .name("John Doe")
                .phone("+573001234567")
                .passwordHash("hash")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Passenger givenPassenger() {
        return givenPassenger(1L, "Juan Pérez", "CC", "123456789", null);
    }

    private Passenger givenPassenger(Long id, String fullName, String docType, 
                                     String docNumber, User user) {
        return Passenger.builder()
                .id(id)
                .fullName(fullName)
                .documentType(docType)
                .documentNumber(docNumber)
                .birthDate(LocalDate.of(1990, 5, 15))
                .phoneNumber("+573001234567")
                .createdAt(OffsetDateTime.now())
                .user(user)
                .build();
    }

    private PassengerDtos.PassengerCreateRequest givenCreateRequest() {
        return new PassengerDtos.PassengerCreateRequest(
                "Juan Pérez",
                "CC",
                "123456789",
                LocalDate.of(1990, 5, 15),
                "+573001234567",
                null  // sin userId
        );
    }

    private PassengerDtos.PassengerCreateRequest givenCreateRequestWithUser(Long userId) {
        return new PassengerDtos.PassengerCreateRequest(
                "María López",
                "CC",
                "987654321",
                LocalDate.of(1985, 3, 20),
                "+573007654321",
                userId
        );
    }

    private PassengerDtos.PassengerUpdateRequest givenUpdateRequest() {
        return new PassengerDtos.PassengerUpdateRequest(
                "Juan Pérez Updated",
                "TI",
                "999888777",
                LocalDate.of(1990, 5, 15),
                "+573009999999"
        );
    }

    @Test
    @DisplayName("Debe crear un pasajero sin usuario asociado")
    void shouldCreatePassengerWithoutUser() {
        // Given
        PassengerDtos.PassengerCreateRequest request = givenCreateRequest();
        Passenger savedPassenger = givenPassenger();

        when(passengerRepository.save(any(Passenger.class))).thenReturn(savedPassenger);

        // When
        PassengerDtos.PassengerResponse response = passengerService.createPassenger(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.fullName()).isEqualTo("Juan Pérez");
        assertThat(response.documentNumber()).isEqualTo("123456789");

        verify(passengerRepository, times(1)).save(any(Passenger.class));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Debe crear un pasajero con usuario asociado")
    void shouldCreatePassengerWithUser() {
        // Given
        User user = givenUser();
        PassengerDtos.PassengerCreateRequest request = givenCreateRequestWithUser(1L);
        Passenger savedPassenger = givenPassenger(1L, "María López", "CC", "987654321", user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.save(any(Passenger.class))).thenReturn(savedPassenger);

        // When
        PassengerDtos.PassengerResponse response = passengerService.createPassenger(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("María López");

        verify(userRepository, times(1)).findById(1L);
        verify(passengerRepository, times(1)).save(any(Passenger.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el userId no existe")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        PassengerDtos.PassengerCreateRequest request = givenCreateRequestWithUser(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> passengerService.createPassenger(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuario con ID 999 no encontrado");

        verify(userRepository, times(1)).findById(999L);
        verify(passengerRepository, never()).save(any(Passenger.class));
    }

    @Test
    @DisplayName("Debe actualizar un pasajero correctamente")
    void shouldUpdatePassenger() {
        // Given
        Passenger existingPassenger = givenPassenger();
        PassengerDtos.PassengerUpdateRequest request = givenUpdateRequest();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(existingPassenger));
        when(passengerRepository.save(any(Passenger.class))).thenReturn(existingPassenger);

        // When
        passengerService.updatePassenger(1L, request);

        // Then
        verify(passengerRepository, times(1)).findById(1L);
        verify(passengerRepository, times(1)).save(existingPassenger);
    }



    @Test
    @DisplayName("Debe actualizar solo los campos no nulos")
    void shouldUpdateOnlyNonNullFields() {
        // Given
        Passenger existingPassenger = givenPassenger();
        PassengerDtos.PassengerUpdateRequest partialRequest = new PassengerDtos.PassengerUpdateRequest(
                "Nuevo Nombre",  // solo actualizar nombre
                null,
                null,
                null,
                null
        );

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(existingPassenger));
        when(passengerRepository.save(any(Passenger.class))).thenReturn(existingPassenger);

        // When
        passengerService.updatePassenger(1L, partialRequest);

        // Then
        verify(passengerRepository, times(1)).save(existingPassenger);
    }

    @Test
    @DisplayName("Debe eliminar un pasajero correctamente")
    void shouldDeletePassenger() {
        // Given
        when(passengerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(passengerRepository).deleteById(1L);

        // When
        passengerService.deletePassenger(1L);

        // Then
        verify(passengerRepository, times(1)).existsById(1L);
        verify(passengerRepository, times(1)).deleteById(1L);
    }



    @Test
    @DisplayName("Debe obtener pasajeros por userId")
    void shouldGetPassengersByUserId() {
        // Given
        User user = givenUser();
        List<Passenger> passengers = List.of(
            givenPassenger(1L, "Pass 1", "CC", "111", user),
            givenPassenger(2L, "Pass 2", "CC", "222", user)
        );

        when(passengerRepository.findByUserId(1L)).thenReturn(passengers);

        // When
        List<PassengerDtos.PassengerResponse> result = passengerService.getPassengerByUser(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PassengerDtos.PassengerResponse::fullName)
                .containsExactly("Pass 1", "Pass 2");

        verify(passengerRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando el usuario no tiene pasajeros")
    void shouldReturnEmptyListWhenUserHasNoPassengers() {
        // Given
        when(passengerRepository.findByUserId(1L)).thenReturn(List.of());

        // When
        List<PassengerDtos.PassengerResponse> result = passengerService.getPassengerByUser(1L);

        // Then
        assertThat(result).isEmpty();
        verify(passengerRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe obtener pasajero por número de documento")
    void shouldGetPassengerByDocumentNumber() {
        // Given
        Passenger passenger = givenPassenger();

        when(passengerRepository.findByDocumentNumber("123456789"))
                .thenReturn(Optional.of(passenger));

        // When
        PassengerDtos.PassengerResponse result = 
            passengerService.finByDocumentNumber("123456789");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.documentNumber()).isEqualTo("123456789");

        verify(passengerRepository, times(1)).findByDocumentNumber("123456789");
    }



    @Test
    @DisplayName("Debe obtener pasajero por ID")
    void shouldGetPassengerById() {
        // Given
        Passenger passenger = givenPassenger();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        // When
        PassengerDtos.PassengerResponse result = passengerService.getPassengerById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("Juan Pérez");

        verify(passengerRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el ID no existe")
    void shouldThrowExceptionWhenIdNotFound() {
        // Given
        when(passengerRepository.findById(999L)).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> passengerService.getPassengerById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Passenger not found");

        verify(passengerRepository, times(1)).findById(999L);
    }
}
