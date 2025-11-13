package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatHoldDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.SeatHoldRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.SeatHoldMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldService Unit Tests")
class SeatHoldServiceImplTest {

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConfigService configService;

    private final SeatHoldMapper seatHoldMapper = Mappers.getMapper(SeatHoldMapper.class);

    private SeatHoldServiceImpl seatHoldService;

    @BeforeEach
    void setUp() {
        seatHoldService = new SeatHoldServiceImpl(
                seatHoldRepository,
                tripRepository,
                userRepository,
                seatHoldMapper,
                configService
        );
    }

    private User givenUser() {
        return User.builder()
                .id(1L)
                .name("testuser")
                .email("test@example.com")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .passwordHash("hash")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Trip givenTrip() {
        Route route = Route.builder()
                .id(1L)
                .code("R001")
                .origin("City A")
                .destination("City B")
                .build();

        return Trip.builder()
                .id(1L)
                .route(route)
                .date(LocalDate.now())
                .departureAt(OffsetDateTime.now().plusDays(1))
                .status(Trip.Status.SCHEDULED)
                .build();
    }

    private SeatHold givenSeatHold(User user, Trip trip, SeatHold.Status status) {
        return SeatHold.builder()
                .id(1L)
                .seatNumber("A1")
                .user(user)
                .trip(trip)
                .status(status)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
    }

    private SeatHoldDtos.SeatHoldCreateRequest givenCreateRequest() {
        return new SeatHoldDtos.SeatHoldCreateRequest(
                "A1",
                1L,
                1L
        );
    }

    @Test
    @DisplayName("Debe crear seat hold correctamente")
    void shouldCreateSeatHold() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHoldDtos.SeatHoldCreateRequest request = givenCreateRequest();
        SeatHold savedSeatHold = givenSeatHold(user, trip, SeatHold.Status.HOLD);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                anyLong(), anyString(), any(), any())).thenReturn(false);
        when(configService.getValueAsInt("HOLD_TIME_MIN")).thenReturn(10);
        when(seatHoldRepository.save(any(SeatHold.class))).thenReturn(savedSeatHold);

        // When
        SeatHoldDtos.SeatHoldResponse response = seatHoldService.createSeatHold(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.seatNumber()).isEqualTo("A1");

        verify(tripRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(seatHoldRepository, times(1)).save(any(SeatHold.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando trip no existe")
    void shouldThrowExceptionWhenTripNotFound() {
        // Given
        SeatHoldDtos.SeatHoldCreateRequest request = givenCreateRequest();

        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatHoldService.createSeatHold(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Viaje no encontrado");

        verify(tripRepository, times(1)).findById(1L);
        verify(seatHoldRepository, never()).save(any(SeatHold.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando asiento ya está en hold")
    void shouldThrowExceptionWhenSeatAlreadyOnHold() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHoldDtos.SeatHoldCreateRequest request = givenCreateRequest();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                anyLong(), anyString(), any(), any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> seatHoldService.createSeatHold(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El asiento A1 ya está reservado");

        verify(seatHoldRepository, never()).save(any(SeatHold.class));
    }

    @Test
    @DisplayName("Debe liberar seat hold correctamente")
    void shouldReleaseSeatHold() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold seatHold = givenSeatHold(user, trip, SeatHold.Status.HOLD);

        when(seatHoldRepository.findById(1L)).thenReturn(Optional.of(seatHold));
        when(seatHoldRepository.save(any(SeatHold.class))).thenReturn(seatHold);

        // When
        seatHoldService.releaseSeatHold(1L);

        // Then
        verify(seatHoldRepository, times(1)).findById(1L);
        verify(seatHoldRepository, times(1)).save(seatHold);
    }

    @Test
    @DisplayName("Debe lanzar excepción al liberar seat hold inexistente")
    void shouldThrowExceptionWhenReleasingNonExistentSeatHold() {
        // Given
        when(seatHoldRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatHoldService.releaseSeatHold(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reserva de asiento no encontrada");

        verify(seatHoldRepository, never()).save(any(SeatHold.class));
    }

    @Test
    @DisplayName("Debe obtener seat hold por ID")
    void shouldGetHoldById() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold seatHold = givenSeatHold(user, trip, SeatHold.Status.HOLD);

        when(seatHoldRepository.findById(1L)).thenReturn(Optional.of(seatHold));

        // When
        SeatHoldDtos.SeatHoldResponse response = seatHoldService.getHoldById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.seatNumber()).isEqualTo("A1");

        verify(seatHoldRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe obtener active holds por trip y user")
    void shouldGetActiveHoldsByTripAndUser() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        List<SeatHold> holds = List.of(givenSeatHold(user, trip, SeatHold.Status.HOLD));

        when(seatHoldRepository.findByTripIdAndUserIdAndStatusAndExpiresAtAfter(
                anyLong(), anyLong(), any(), any())).thenReturn(holds);

        // When
        List<SeatHoldDtos.SeatHoldResponse> result =
                seatHoldService.getActiveHoldsByTripAndUser(1L, 1L);

        // Then
        assertThat(result).hasSize(1);

        verify(seatHoldRepository, times(1))
                .findByTripIdAndUserIdAndStatusAndExpiresAtAfter(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Debe obtener active holds por trip")
    void shouldGetActiveHoldsByTrip() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        List<SeatHold> holds = List.of(givenSeatHold(user, trip, SeatHold.Status.HOLD));

        when(seatHoldRepository.findByTripIdAndStatusAndExpiresAtAfter(
                anyLong(), any(), any())).thenReturn(holds);

        // When
        List<SeatHoldDtos.SeatHoldResponse> result = seatHoldService.getActiveHoldsByTrip(1L);

        // Then
        assertThat(result).hasSize(1);

        verify(seatHoldRepository, times(1))
                .findByTripIdAndStatusAndExpiresAtAfter(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Debe obtener holds por user")
    void shouldGetHoldsByUser() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        List<SeatHold> holds = List.of(givenSeatHold(user, trip, SeatHold.Status.HOLD));

        when(seatHoldRepository.findByUserId(1L)).thenReturn(holds);

        // When
        List<SeatHoldDtos.SeatHoldResponse> result = seatHoldService.getHoldsByUser(1L);

        // Then
        assertThat(result).hasSize(1);

        verify(seatHoldRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe verificar si asiento está en hold")
    void shouldCheckIfSeatIsOnHold() {
        // Given
        when(seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                anyLong(), anyString(), any(), any())).thenReturn(true);

        // When
        boolean result = seatHoldService.isSeatOnHold(1L, "A1");

        // Then
        assertThat(result).isTrue();

        verify(seatHoldRepository, times(1))
                .existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Debe marcar holds expirados")
    void shouldMarkExpiredHolds() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold expiredHold = givenSeatHold(user, trip, SeatHold.Status.HOLD);
        expiredHold.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        List<SeatHold> expiredHolds = new ArrayList<>(List.of(expiredHold));

        when(seatHoldRepository.findByStatusAndExpiresAtBefore(any(), any()))
                .thenReturn(expiredHolds);
        when(seatHoldRepository.saveAll(anyList())).thenReturn(expiredHolds);

        // When
        int count = seatHoldService.markExpiredHolds();

        // Then
        assertThat(count).isEqualTo(1);

        verify(seatHoldRepository, times(1)).findByStatusAndExpiresAtBefore(any(), any());
        verify(seatHoldRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe retornar 0 cuando no hay holds expirados para marcar")
    void shouldReturnZeroWhenNoExpiredHoldsToMark() {
        // Given
        when(seatHoldRepository.findByStatusAndExpiresAtBefore(any(), any()))
                .thenReturn(List.of());

        // When
        int count = seatHoldService.markExpiredHolds();

        // Then
        assertThat(count).isEqualTo(0);

        verify(seatHoldRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe eliminar holds expirados")
    void shouldDeleteExpiredHolds() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        List<SeatHold> expiredHolds = List.of(givenSeatHold(user, trip, SeatHold.Status.EXPIRED));

        when(seatHoldRepository.findByStatus(SeatHold.Status.EXPIRED)).thenReturn(expiredHolds);
        doNothing().when(seatHoldRepository).deleteAll(anyList());

        // When
        int count = seatHoldService.deleteExpiredHolds();

        // Then
        assertThat(count).isEqualTo(1);

        verify(seatHoldRepository, times(1)).findByStatus(SeatHold.Status.EXPIRED);
        verify(seatHoldRepository, times(1)).deleteAll(anyList());
    }

    @Test
    @DisplayName("Debe retornar 0 cuando no hay holds expirados para eliminar")
    void shouldReturnZeroWhenNoExpiredHoldsToDelete() {
        // Given
        when(seatHoldRepository.findByStatus(SeatHold.Status.EXPIRED)).thenReturn(List.of());

        // When
        int count = seatHoldService.deleteExpiredHolds();

        // Then
        assertThat(count).isEqualTo(0);

        verify(seatHoldRepository, never()).deleteAll(anyList());
    }

    @Test
    @DisplayName("Debe validar active holds correctamente")
    void shouldValidateActiveHolds() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold hold1 = givenSeatHold(user, trip, SeatHold.Status.HOLD);
        SeatHold hold2 = givenSeatHold(user, trip, SeatHold.Status.HOLD);
        hold2.setSeatNumber("A2");

        List<String> seatNumbers = List.of("A1", "A2");
        List<SeatHold> holds = List.of(hold1, hold2);

        when(seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                anyLong(), anyList(), any())).thenReturn(holds);

        // When & Then (no debe lanzar excepción)
        seatHoldService.validateActiveHolds(1L, seatNumbers, 1L);

        verify(seatHoldRepository, times(1))
                .findByTripIdAndSeatNumberInAndStatus(anyLong(), anyList(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando no se encuentran reservas")
    void shouldThrowExceptionWhenNoHoldsFound() {
        // Given
        List<String> seatNumbers = List.of("A1", "A2");

        when(seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                anyLong(), anyList(), any())).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> seatHoldService.validateActiveHolds(1L, seatNumbers, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se encontraron reservas activas");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando reserva no pertenece al usuario")
    void shouldThrowExceptionWhenHoldNotOwnedByUser() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold hold = givenSeatHold(user, trip, SeatHold.Status.HOLD);

        List<String> seatNumbers = List.of("A1");

        when(seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                anyLong(), anyList(), any())).thenReturn(List.of(hold));

        // When & Then
        assertThatThrownBy(() -> seatHoldService.validateActiveHolds(1L, seatNumbers, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pertenece al usuario");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando reserva ha expirado")
    void shouldThrowExceptionWhenHoldExpired() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        SeatHold hold = givenSeatHold(user, trip, SeatHold.Status.HOLD);
        hold.setExpiresAt(OffsetDateTime.now().minusMinutes(5)); // Expirado

        List<String> seatNumbers = List.of("A1");

        when(seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                anyLong(), anyList(), any())).thenReturn(List.of(hold));

        // When & Then
        assertThatThrownBy(() -> seatHoldService.validateActiveHolds(1L, seatNumbers, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ha expirado");
    }

    @Test
    @DisplayName("Debe calcular tiempo de expiración correctamente")
    void shouldCalculateExpirationTime() {
        // Given
        when(configService.getValueAsInt("HOLD_TIME_MIN")).thenReturn(10);

        // When
        OffsetDateTime expiresAt = seatHoldService.calculateExpirationTime();

        // Then
        assertThat(expiresAt).isAfter(OffsetDateTime.now());

        verify(configService, times(1)).getValueAsInt("HOLD_TIME_MIN");
    }

    @Test
    @DisplayName("Debe verificar si existe overlapping hold")
    void shouldCheckForOverlappingHold() {
        // Given
        when(seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                anyLong(), anyString(), any(), any())).thenReturn(true);

        // When
        boolean result = seatHoldService.hasOverlappingHold(1L, "A1", 1, 3);

        // Then
        assertThat(result).isTrue();

        verify(seatHoldRepository, times(1))
                .existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(anyLong(), anyString(), any(), any());
    }
}