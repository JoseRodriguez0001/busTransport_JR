package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.TripMapper;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private BusRepository busRepository;
    @Spy
    private TripMapper tripMapper = Mappers.getMapper(TripMapper.class);
    @InjectMocks
    private TripServiceImpl tripService;

    private Route route;
    private Bus bus;
    private Trip trip;
    private LocalDate today;
    private LocalDate tomorrow;
    private OffsetDateTime departureAt;
    private OffsetDateTime arrivalAt;
    private TripDtos.TripCreateRequest createRequest;
    private TripDtos.TripUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {

        // fechas
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        departureAt = OffsetDateTime.of(today.atTime(8, 0), ZoneOffset.UTC);
        arrivalAt = OffsetDateTime.of(today.atTime(10, 0), ZoneOffset.UTC);

        // Preparar entidades
        route = createRoute(1L, "R001", "Santa Marta", "Barranquilla");
        bus = createBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        trip = createTrip(1L, route, bus, today, departureAt, arrivalAt, Trip.Status.SCHEDULED);

        createRequest = new TripDtos.TripCreateRequest(
                1L,
                1L,
                tomorrow,
                OffsetDateTime.of(tomorrow.atTime(14, 0), ZoneOffset.UTC),
                OffsetDateTime.of(tomorrow.atTime(16, 0), ZoneOffset.UTC),
                5.0
        );

        updateRequest = new TripDtos.TripUpdateRequest(
                OffsetDateTime.of(today.atTime(9, 0), ZoneOffset.UTC),
                OffsetDateTime.of(today.atTime(11, 0), ZoneOffset.UTC),
                10.0,
                Trip.Status.BOARDING
        );
    }

    @Test
    @DisplayName("Debe crear un trip exitosamente")
    void createTrip_ShouldCreateSuccessfully() {
        // Given
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(tripRepository.findByBusIdAndStatus(eq(1L), any())).thenReturn(new ArrayList<>());
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(tripRepository.countSoldTickets(anyLong())).thenReturn(0L);

        // When
        TripDtos.TripResponse response = tripService.createTrip(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.date()).isEqualTo(tomorrow);
        assertThat(response.status()).isEqualTo("SCHEDULED");

        verify(routeRepository).findById(1L);
        verify(busRepository).findById(1L);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si la ruta no existe")
    void createTrip_ShouldThrowNotFoundException_WhenRouteDoesNotExist() {
        // Given
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tripService.createTrip(createRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Route with ID 1 not found");

        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el bus no existe")
    void createTrip_ShouldThrowNotFoundException_WhenBusDoesNotExist() {
        // Given
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(busRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tripService.createTrip(createRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 1 not found");

        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar un trip exitosamente")
    void updateTrip_ShouldUpdateSuccessfully() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(0L);
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        // When
        tripService.updateTrip(1L, updateRequest);

        // Then
        verify(tripRepository).findById(1L);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el trip no existe")
    void updateTrip_ShouldThrowNotFoundException_WhenTripDoesNotExist() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tripService.updateTrip(1L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trip with ID 1 not found");

        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si intenta cambiar fechas con tickets vendidos")
    void updateTrip_ShouldThrowException_WhenChangingDatesWithSoldTickets() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> tripService.updateTrip(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot modify dates");

        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe validar transición de estado inválida")
    void updateTrip_ShouldThrowException_WhenInvalidStatusTransition() {
        // Given - Trip ARRIVED no puede cambiar a BOARDING
        Trip arrivedTrip = createTrip(1L, route, bus, today, departureAt, arrivalAt, Trip.Status.ARRIVED);
        TripDtos.TripUpdateRequest invalidUpdate = new TripDtos.TripUpdateRequest(
                null,
                null,
                null,
                Trip.Status.BOARDING // Transición inválida
        );

        when(tripRepository.findById(1L)).thenReturn(Optional.of(arrivedTrip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(0L);

        // When & Then
        assertThatThrownBy(() -> tripService.updateTrip(1L, invalidUpdate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot change status of an arrived trip");

        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar un trip exitosamente")
    void deleteTrip_ShouldDeleteSuccessfully() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(0L);

        // When
        tripService.deleteTrip(1L);

        // Then
        verify(tripRepository).findById(1L);
        verify(tripRepository).delete(trip);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el trip no existe")
    void deleteTrip_ShouldThrowNotFoundException_WhenTripDoesNotExist() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tripService.deleteTrip(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trip with ID 1 not found");

        verify(tripRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si intenta eliminar trip con tickets vendidos")
    void deleteTrip_ShouldThrowException_WhenHasSoldTickets() {
        // Given
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(10L);

        // When & Then
        assertThatThrownBy(() -> tripService.deleteTrip(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete trip");

        verify(tripRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe buscar trips por origen, destino y fecha")
    void getTrips_ShouldReturnTrips() {
        // Given
        Trip trip2 = createTrip(2L, route, bus, today, 
                OffsetDateTime.of(today.atTime(14, 0), ZoneOffset.UTC),
                OffsetDateTime.of(today.atTime(16, 0), ZoneOffset.UTC),
                Trip.Status.SCHEDULED);

        when(tripRepository.findTripsByOriginAndDestination("Santa Marta", "Barranquilla", today))
                .thenReturn(List.of(trip, trip2));
        // When
        List<TripDtos.TripResponse> responses = tripService.getTrips("Santa Marta", "Barranquilla", today);

        // Then
        assertThat(responses).hasSize(2);
        verify(tripRepository).findTripsByOriginAndDestination("Santa Marta", "Barranquilla", today);
    }

    @Test
    @DisplayName("Debe lanzar excepción si los parámetros son inválidos")
    void getTrips_ShouldThrowException_WhenParametersInvalid() {
        // When & Then - Origen vacío
        assertThatThrownBy(() -> tripService.getTrips("", "Barranquilla", today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin cannot be null or empty");

        // When & Then - Destino nulo
        assertThatThrownBy(() -> tripService.getTrips("Santa Marta", null, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination cannot be null or empty");

        // When & Then - Fecha nula
        assertThatThrownBy(() -> tripService.getTrips("Santa Marta", "Barranquilla", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date cannot be null");
    }

    @Test
    @DisplayName("Debe obtener detalles del trip con bus y seats")
    void getTripDetails_ShouldReturnTripWithBusAndSeats() {
        // Given
        Seat seat1 = createSeat(1L, "1A", bus);
        Seat seat2 = createSeat(2L, "1B", bus);
        bus.setSeats(List.of(seat1, seat2));

        when(tripRepository.findByIdWithBusAndSeats(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.countSoldTickets(1L)).thenReturn(0L);

        // When
        TripDtos.TripResponse response = tripService.getTripDetails(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        verify(tripRepository).findByIdWithBusAndSeats(1L);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el trip no existe")
    void getTripDetails_ShouldThrowNotFoundException_WhenTripDoesNotExist() {
        // Given
        when(tripRepository.findByIdWithBusAndSeats(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tripService.getTripDetails(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trip with 1 not found");
    }

    @Test
    @DisplayName("Debe obtener lista de asientos con disponibilidad")
    void getSeats_ShouldReturnSeatsWithAvailability() {
        // Given
        Seat seat1 = createSeat(1L, "1A", bus);
        Seat seat2 = createSeat(2L, "1B", bus);
        bus.setSeats(List.of(seat1, seat2));

        when(tripRepository.findByIdWithBusAndSeats(1L)).thenReturn(Optional.of(trip));

        // When
        List<SeatDtos.SeatResponse> responses = tripService.getSeats(1L);

        // Then
        assertThat(responses)
                .hasSize(2)
                .extracting(SeatDtos.SeatResponse::number)
                .containsExactly("1A", "1B");

        verify(tripRepository).findByIdWithBusAndSeats(1L);
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el bus no tiene asientos configurados")
    void getSeats_ShouldReturnEmptyList_WhenBusHasNoSeats() {
        // Given
        bus.setSeats(List.of());
        when(tripRepository.findByIdWithBusAndSeats(1L)).thenReturn(Optional.of(trip));

        // When
        List<SeatDtos.SeatResponse> responses = tripService.getSeats(1L);

        // Then
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar estadísticas del trip")
    void getTripStatistics_ShouldReturnSoldTicketsCount() {
        // Given
        when(tripRepository.existsById(1L)).thenReturn(true);
        when(tripRepository.countSoldTickets(1L)).thenReturn(15L);

        // When
        Long soldTickets = tripService.getTripStatistics(1L);

        // Then
        assertThat(soldTickets).isEqualTo(15L);
        verify(tripRepository).countSoldTickets(1L);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el trip no existe")
    void getTripStatistics_ShouldThrowNotFoundException_WhenTripDoesNotExist() {
        // Given
        when(tripRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> tripService.getTripStatistics(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trip with ID 1 not found");
    }

    private Route createRoute(Long id, String code, String origin, String destination) {
        Route route = new Route();
        route.setId(id);
        route.setCode(code);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setName(origin + " - " + destination);
        route.setDistanceKm(100.0);
        route.setDurationMin(120);
        return route;
    }

    private Bus createBus(Long id, String plate, Integer capacity, Bus.Status status) {
        Bus bus = new Bus();
        bus.setId(id);
        bus.setPlate(plate);
        bus.setCapacity(capacity);
        bus.setStatus(status);
        return bus;
    }

    private Trip createTrip(Long id, Route route, Bus bus, LocalDate date, 
                           OffsetDateTime departure, OffsetDateTime arrival, Trip.Status status) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setRoute(route);
        trip.setBus(bus);
        trip.setDate(date);
        trip.setDepartureAt(departure);
        trip.setArrivalAt(arrival);
        trip.setStatus(status);
        trip.setOverbookingPercent(0.0);
        trip.setTickets(List.of());
        trip.setSeatHolds(List.of());
        trip.setParcels(List.of());
        return trip;
    }

    private Seat createSeat(Long id, String number, Bus bus) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setNumber(number);
        seat.setType(Seat.Type.STANDARD);
        seat.setBus(bus);
        return seat;
    }
}