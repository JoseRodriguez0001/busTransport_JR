package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.SeatHoldService;
import com.unimag.bustransport.services.mapper.SeatMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private SeatHoldService seatHoldService;

    private final SeatMapper seatMapper = Mappers.getMapper(SeatMapper.class);

    @InjectMocks
    private SeatServiceImpl seatService;

    private Bus bus;
    private Seat seat;
    private SeatDtos.SeatCreateRequest createRequest;
    private SeatDtos.SeatUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        seatService = new SeatServiceImpl(
                seatRepository,
                busRepository,
                tripRepository,
                ticketRepository,
                stopRepository,
                purchaseRepository,
                seatHoldService,
                seatMapper
        );

        // Preparar datos de prueba
        bus = createBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        seat = createSeat(1L, "1A", Seat.Type.STANDARD, bus);
        createRequest = new SeatDtos.SeatCreateRequest("2A", Seat.Type.STANDARD, 1L);
        updateRequest = new SeatDtos.SeatUpdateRequest("2B", Seat.Type.PREFERENTIAL);
    }

    @Test
    @DisplayName("Debe crear un asiento exitosamente")
    void createSeat_ShouldCreateSuccessfully() {
        // Given
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(seatRepository.findByBusIdAndNumber(1L, "2A")).thenReturn(Optional.empty());
        when(seatRepository.countByBusId(1L)).thenReturn(10L);
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> {
            Seat saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        SeatDtos.SeatResponse response = seatService.createSeat(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.number()).isEqualTo("2A");
        assertThat(response.type()).isEqualTo("STANDARD");
        assertThat(response.busPlate()).isEqualTo("ABC123");

        verify(busRepository).findById(1L);
        verify(seatRepository).findByBusIdAndNumber(1L, "2A");
        verify(seatRepository).countByBusId(1L);
        verify(seatRepository).save(any(Seat.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el bus no existe")
    void createSeat_ShouldThrowNotFoundException_WhenBusDoesNotExist() {
        // Given
        when(busRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.createSeat(createRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 1 not found");

        verify(busRepository).findById(1L);
        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException si el asiento ya existe")
    void createSeat_ShouldThrowDuplicateException_WhenSeatAlreadyExists() {
        // Given
        Seat existingSeat = createSeat(2L, "2A", Seat.Type.STANDARD, bus);
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(seatRepository.findByBusIdAndNumber(1L, "2A")).thenReturn(Optional.of(existingSeat));

        // When & Then
        assertThatThrownBy(() -> seatService.createSeat(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Seat 2A already exists");

        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el bus alcanzó su capacidad")
    void createSeat_ShouldThrowException_WhenBusAtCapacity() {
        // Given
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(seatRepository.findByBusIdAndNumber(1L, "2A")).thenReturn(Optional.empty());
        when(seatRepository.countByBusId(1L)).thenReturn(40L); // Ya tiene 40 asientos

        // When & Then
        assertThatThrownBy(() -> seatService.createSeat(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reached its capacity");

        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el formato del número es inválido")
    void createSeat_ShouldThrowException_WhenSeatNumberFormatInvalid() {
        // Given
        SeatDtos.SeatCreateRequest invalidRequest = new SeatDtos.SeatCreateRequest(
                "INVALID",
                Seat.Type.STANDARD,
                1L
        );
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

        // When & Then
        assertThatThrownBy(() -> seatService.createSeat(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid seat number format");

        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar un asiento exitosamente")
    void updateSeat_ShouldUpdateSuccessfully() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);

        // When
        seatService.updateSeat(1L, updateRequest);

        // Then
        verify(seatRepository).findById(1L);
        verify(seatRepository).save(any(Seat.class));
        assertThat(seat.getNumber()).isEqualTo("2B");
        assertThat(seat.getType()).isEqualTo(Seat.Type.PREFERENTIAL);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el asiento no existe")
    void updateSeat_ShouldThrowNotFoundException_WhenSeatDoesNotExist() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.updateSeat(1L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Seat with ID 1 not found");

        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si intenta cambiar número y hay tickets vendidos")
    void updateSeat_ShouldThrowException_WhenChangingNumberWithSoldTickets() {
        // Given
        SeatDtos.SeatUpdateRequest changeNumberRequest = new SeatDtos.SeatUpdateRequest(
                "3A",
                null
        );
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));
        when(ticketRepository.existsByTripIdAndSeatNumberAndStatus(anyLong(), eq("1A"), any()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> seatService.updateSeat(1L, changeNumberRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has sold tickets");

        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar un asiento exitosamente")
    void deleteSeat_ShouldDeleteSuccessfully() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));
        when(ticketRepository.findByTripIdAndSeatNumberIn(isNull(), anyList()))
                .thenReturn(List.of());
        when(seatHoldService.isSeatOnHold(isNull(), eq("1A"))).thenReturn(false);

        // When
        seatService.deleteSeat(1L);

        // Then
        verify(seatRepository).findById(1L);
        verify(seatRepository).delete(seat);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el asiento no existe")
    void deleteSeat_ShouldThrowNotFoundException_WhenSeatDoesNotExist() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.deleteSeat(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Seat with ID 1 not found");

        verify(seatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe obtener todos los asientos de un bus")
    void getSeatsByBusId_ShouldReturnAllSeats() {
        // Given
        Seat seat2 = createSeat(2L, "1B", Seat.Type.PREFERENTIAL, bus);
        when(busRepository.existsById(1L)).thenReturn(true);
        when(seatRepository.findByBusIdOrderByNumberAsc(1L))
                .thenReturn(List.of(seat, seat2));

        // When
        List<SeatDtos.SeatResponse> responses = seatService.getSeatsByBusId(1L);

        // Then
        assertThat(responses)
                .hasSize(2)
                .extracting(SeatDtos.SeatResponse::number)
                .containsExactly("1A", "1B");

        verify(busRepository).existsById(1L);
        verify(seatRepository).findByBusIdOrderByNumberAsc(1L);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el bus no existe")
    void getSeatsByBusId_ShouldThrowNotFoundException_WhenBusDoesNotExist() {
        // Given
        when(busRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> seatService.getSeatsByBusId(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 1 not found");

        verify(seatRepository, never()).findByBusIdOrderByNumberAsc(anyLong());
    }

    @Test
    @DisplayName("Debe obtener asientos de un tipo específico")
    void getSeatsByBusIdAndType_ShouldReturnSeatsByType() {
        // Given
        when(busRepository.existsById(1L)).thenReturn(true);
        when(seatRepository.findByBusIdAndType(1L, Seat.Type.STANDARD))
                .thenReturn(List.of(seat));

        // When
        List<SeatDtos.SeatResponse> responses = seatService.getSeatsByBusIdAndType(
                1L,
                Seat.Type.STANDARD
        );

        // Then
        assertThat(responses)
                .hasSize(1)
                .extracting(SeatDtos.SeatResponse::type)
                .containsExactly("STANDARD");

        verify(seatRepository).findByBusIdAndType(1L, Seat.Type.STANDARD);
    }

    @Test
    @DisplayName("Debe obtener un asiento por ID")
    void getSeatById_ShouldReturnSeat() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));

        // When
        SeatDtos.SeatResponse response = seatService.getSeatById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.number()).isEqualTo("1A");

        verify(seatRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el asiento no existe")
    void getSeatById_ShouldThrowNotFoundException_WhenSeatDoesNotExist() {
        // Given
        when(seatRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.getSeatById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Seat with ID 1 not found");
    }

    @Test
    @DisplayName("Debe retornar true si el asiento está disponible")
    void isSeatAvailable_ShouldReturnTrue_WhenSeatIsAvailable() {
        // Given
        Trip trip = createTrip(1L);
        Stop fromStop = createStop(1L, 0);
        Stop toStop = createStop(2L, 2);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(seatRepository.findByBusIdAndNumber(anyLong(), eq("1A")))
                .thenReturn(Optional.of(seat));
        when(ticketRepository.findOverlappingTickets(anyLong(), eq("1A"), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(seatHoldService.hasOverlappingHold(anyLong(), eq("1A"), anyInt(), anyInt()))
                .thenReturn(false);

        // When
        boolean available = seatService.isSeatAvailable(1L, "1A", 1L, 2L);

        // Then
        assertThat(available).isTrue();

        verify(tripRepository).findById(1L);
        verify(ticketRepository).findOverlappingTickets(anyLong(), eq("1A"), anyInt(), anyInt());
        verify(seatHoldService).hasOverlappingHold(anyLong(), eq("1A"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Debe retornar false si el asiento no existe en el bus")
    void isSeatAvailable_ShouldReturnFalse_WhenSeatDoesNotExist() {
        // Given
        Trip trip = createTrip(1L);
        Stop fromStop = createStop(1L, 0);
        Stop toStop = createStop(2L, 2);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(seatRepository.findByBusIdAndNumber(anyLong(), eq("99Z")))
                .thenReturn(Optional.empty());

        // When
        boolean available = seatService.isSeatAvailable(1L, "99Z", 1L, 2L);

        // Then
        assertThat(available).isFalse();

        verify(ticketRepository, never()).findOverlappingTickets(anyLong(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Debe lanzar excepción si fromStop order >= toStop order")
    void isSeatAvailable_ShouldThrowException_WhenInvalidStopOrder() {
        // Given
        Trip trip = createTrip(1L);
        Stop fromStop = createStop(1L, 2);
        Stop toStop = createStop(2L, 1); // order inválido

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));

        // When & Then
        assertThatThrownBy(() -> seatService.isSeatAvailable(1L, "1A", 1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromStop order must be less than toStop order");
    }

    private Bus createBus(Long id, String plate, Integer capacity, Bus.Status status) {
        Bus bus = new Bus();
        bus.setId(id);
        bus.setPlate(plate);
        bus.setCapacity(capacity);
        bus.setStatus(status);
        return bus;
    }

    private Seat createSeat(Long id, String number, Seat.Type type, Bus bus) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setNumber(number);
        seat.setType(type);
        seat.setBus(bus);
        return seat;
    }

    private Trip createTrip(Long id) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setBus(bus);
        trip.setRoute(createRoute());
        return trip;
    }

    private com.unimag.bustransport.domain.entities.Route createRoute() {
        com.unimag.bustransport.domain.entities.Route route = new com.unimag.bustransport.domain.entities.Route();
        route.setId(1L);
        route.setOrigin("Santa Marta");
        route.setDestination("Barranquilla");
        return route;
    }

    private Stop createStop(Long id, Integer order) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setOrder(order);
        stop.setRoute(createRoute());
        return stop;
    }
}