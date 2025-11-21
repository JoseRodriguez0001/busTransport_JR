package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.SeatRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.BusMapper;
import com.unimag.bustransport.services.mapper.SeatMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusServiceImplTest {

    @Mock
    private BusRepository busRepository;

    @Mock
    private SeatRepository seatRepository;

    @Spy
    private BusMapper busMapper = Mappers.getMapper(BusMapper.class);

    @Spy
    private SeatMapper seatMapper = Mappers.getMapper(SeatMapper.class);

    @InjectMocks
    private BusServiceImpl busService;

    @Captor
    private ArgumentCaptor<List<Seat>> seatsCaptor;

    private Bus givenBus(Long id, String plate, Integer capacity, Bus.Status status) {
        return Bus.builder()
                .id(id)
                .plate(plate)
                .capacity(capacity)
                .amenities(new ArrayList<>(List.of("WiFi", "AC")))
                .status(status)
                .seats(new ArrayList<>())
                .trips(new ArrayList<>())
                .build();
    }

    private BusDtos.BusCreateRequest givenCreateRequest(String plate, Integer capacity) {
        return new BusDtos.BusCreateRequest(
                plate,
                capacity,
                List.of("WiFi", "AC"),
                Bus.Status.ACTIVE
        );
    }

    private BusDtos.BusUpdateRequest givenUpdateRequest() {
        return new BusDtos.BusUpdateRequest(
                44,
                List.of("WiFi", "AC", "TV"),
                Bus.Status.IN_REPAIR
        );
    }

    private Seat givenSeat(Long id, String number, Bus bus) {
        return Seat.builder()
                .id(id)
                .number(number)
                .type(Seat.Type.STANDARD)
                .bus(bus)
                .build();
    }

    @Test
    @DisplayName("Debe crear un bus y sus asientos automáticamente")
    void shouldCreateBusWithSeats() {
        // Given
        BusDtos.BusCreateRequest request = givenCreateRequest("ABC123", 40);
        Bus savedBus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);

        when(busRepository.findByPlate("ABC123")).thenReturn(Optional.empty());
        when(busRepository.save(any(Bus.class))).thenReturn(savedBus);
        when(seatRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        BusDtos.BusResponse response = busService.createBus(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.plate()).isEqualTo("ABC123");
        assertThat(response.capacity()).isEqualTo(40);

        verify(busRepository, times(1)).findByPlate("ABC123");
        verify(busRepository, times(1)).save(any(Bus.class));
        verify(seatRepository, times(1)).saveAll(seatsCaptor.capture());

        List<Seat> capturedSeats = seatsCaptor.getValue();
        assertThat(capturedSeats).hasSize(40);
        assertThat(capturedSeats.get(0).getNumber()).isEqualTo("1A");
        assertThat(capturedSeats.get(3).getNumber()).isEqualTo("1D");
        assertThat(capturedSeats.get(39).getNumber()).isEqualTo("10D");
    }

    @Test
    @DisplayName("Debe crear asientos PREFERENCIAL en la primera fila")
    void shouldCreatePreferentialSeatsInFirstRow() {
        // Given
        BusDtos.BusCreateRequest request = givenCreateRequest("XYZ789", 20);
        Bus savedBus = givenBus(1L, "XYZ789", 20, Bus.Status.ACTIVE);

        when(busRepository.findByPlate("XYZ789")).thenReturn(Optional.empty());
        when(busRepository.save(any(Bus.class))).thenReturn(savedBus);
        when(seatRepository.saveAll(anyList())).thenReturn(List.of());

        // When
        busService.createBus(request);

        // Then
        verify(seatRepository, times(1)).saveAll(seatsCaptor.capture());

        List<Seat> capturedSeats = seatsCaptor.getValue();

        assertThat(capturedSeats.subList(0, 4))
                .allMatch(seat -> seat.getType() == Seat.Type.PREFERENTIAL);

        assertThat(capturedSeats.subList(4, capturedSeats.size()))
                .allMatch(seat -> seat.getType() == Seat.Type.STANDARD);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la placa ya existe")
    void shouldThrowExceptionWhenPlateExists() {
        // Given
        BusDtos.BusCreateRequest request = givenCreateRequest("ABC123", 40);
        Bus existingBus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);

        when(busRepository.findByPlate("ABC123")).thenReturn(Optional.of(existingBus));

        // When & Then
        assertThatThrownBy(() -> busService.createBus(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Bus already exists with plate 'ABC123'");

        verify(busRepository, times(1)).findByPlate("ABC123");
        verify(busRepository, never()).save(any(Bus.class));
        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la capacidad no es múltiplo de 4")
    void shouldThrowExceptionWhenCapacityNotMultipleOf4() {
        // Given
        BusDtos.BusCreateRequest request = givenCreateRequest("ABC123", 37);

        when(busRepository.findByPlate("ABC123")).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> busService.createBus(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity must be a multiple of");

        verify(busRepository, never()).save(any(Bus.class));
    }

    @Test
    @DisplayName("Debe actualizar un bus correctamente")
    void shouldUpdateBus() {
        // Given
        Bus existingBus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        BusDtos.BusUpdateRequest request = givenUpdateRequest();

        when(busRepository.findById(1L)).thenReturn(Optional.of(existingBus));
        when(busRepository.save(any(Bus.class))).thenReturn(existingBus);

        // When
        busService.updateBus(1L, request);

        // Then
        verify(busRepository, times(1)).findById(1L);
        verify(busRepository, times(1)).save(existingBus);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar bus inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentBus() {
        // Given
        BusDtos.BusUpdateRequest request = givenUpdateRequest();

        when(busRepository.findById(999L)).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> busService.updateBus(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 999 not found");

        verify(busRepository, times(1)).findById(999L);
        verify(busRepository, never()).save(any(Bus.class));
    }

    @Test
    @DisplayName("Debe validar múltiplo de 4 al cambiar capacidad")
    void shouldValidateMultipleOf4WhenChangingCapacity() {
        // Given
        Bus existingBus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        BusDtos.BusUpdateRequest request = new BusDtos.BusUpdateRequest(
                37,
                List.of("WiFi"),
                Bus.Status.ACTIVE
        );

        when(busRepository.findById(1L)).thenReturn(Optional.of(existingBus));

        // When y Then
        assertThatThrownBy(() -> busService.updateBus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity must be a multiple of");

        verify(busRepository, never()).save(any(Bus.class));
    }

    @Test
    @DisplayName("Debe retirar un bus (soft delete)")
    void shouldRetireBus() {
        // Given
        Bus bus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);

        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(busRepository.save(any(Bus.class))).thenReturn(bus);

        // When
        busService.deleteBus(1L);

        // Then
        verify(busRepository, times(1)).findById(1L);
        verify(busRepository, times(1)).save(bus);
    }

    @Test
    @DisplayName("Debe lanzar excepción al retirar bus inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentBus() {
        // Given
        when(busRepository.findById(999L)).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> busService.deleteBus(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 999 not found");

        verify(busRepository, times(1)).findById(999L);
        verify(busRepository, never()).save(any(Bus.class));
    }

    @Test
    @DisplayName("Debe obtener un bus por ID")
    void shouldGetBusById() {
        // Given
        Bus bus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);

        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

        // When
        BusDtos.BusResponse response = busService.getBus(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.plate()).isEqualTo("ABC123");

        verify(busRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el bus no existe")
    void shouldThrowExceptionWhenBusNotFound() {
        // Given
        when(busRepository.findById(999L)).thenReturn(Optional.empty());

        // When y Then
        assertThatThrownBy(() -> busService.getBus(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 999 not found");

        verify(busRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Debe obtener todos los buses")
    void shouldGetAllBuses() {
        // Given
        List<Bus> buses = List.of(
                givenBus(1L, "BUS001", 40, Bus.Status.ACTIVE),
                givenBus(2L, "BUS002", 44, Bus.Status.ACTIVE)
        );

        when(busRepository.findAll()).thenReturn(buses);

        // When
        List<BusDtos.BusResponse> result = busService.getAllBus();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(BusDtos.BusResponse::plate)
                .containsExactly("BUS001", "BUS002");

        verify(busRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay buses")
    void shouldReturnEmptyListWhenNoBuses() {
        // Given
        when(busRepository.findAll()).thenReturn(List.of());

        // When
        List<BusDtos.BusResponse> result = busService.getAllBus();

        // Then
        assertThat(result).isEmpty();
        verify(busRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe obtener asientos de un bus")
    void shouldGetSeatsByBusId() {
        // Given
        Bus bus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        List<Seat> seats = List.of(
                givenSeat(1L, "1A", bus),
                givenSeat(2L, "1B", bus),
                givenSeat(3L, "1C", bus)
        );
        bus.setSeats(seats);

        when(busRepository.findByIdWithSeats(1L)).thenReturn(Optional.of(bus));

        // When
        List<SeatDtos.SeatResponse> result = busService.getAllSeatsByBusId(1L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(SeatDtos.SeatResponse::number)
                .containsExactly("1A", "1B", "1C");

        verify(busRepository, times(1)).findByIdWithSeats(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción al obtener asientos de bus inexistente")
    void shouldThrowExceptionWhenGettingSeatsOfNonExistentBus() {
        // Given
        when(busRepository.findByIdWithSeats(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> busService.getAllSeatsByBusId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bus with ID 999 not found");

        verify(busRepository, times(1)).findByIdWithSeats(999L);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando el bus no tiene asientos")
    void shouldReturnEmptyListWhenBusHasNoSeats() {
        // Given
        Bus bus = givenBus(1L, "ABC123", 40, Bus.Status.ACTIVE);
        bus.setSeats(new ArrayList<>());

        when(busRepository.findByIdWithSeats(1L)).thenReturn(Optional.of(bus));

        // When
        List<SeatDtos.SeatResponse> result = busService.getAllSeatsByBusId(1L);

        // Then
        assertThat(result).isEmpty();
        verify(busRepository, times(1)).findByIdWithSeats(1L);
    }
}