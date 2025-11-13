package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SeatRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BusRepository busRepository;

    private Bus bus1;
    private Bus bus2;
    private Seat seat1A;
    private Seat seat1B;
    private Seat seat2A;
    private Seat seat2B;
    private Seat seat2C;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        busRepository.deleteAll();

        // Crear datos de prueba
        bus1 = createBus("ABC123", 40, Bus.Status.ACTIVE);
        bus2 = createBus("XYZ789", 30, Bus.Status.ACTIVE);

        // Crear asientos para bus1
        seat1A = createSeat("1A", Seat.Type.STANDARD, bus1);
        seat1B = createSeat("1B", Seat.Type.PREFERENTIAL, bus1);

        // Crear asientos para bus2
        seat2A = createSeat("1A", Seat.Type.STANDARD, bus2);
        seat2B = createSeat("2A", Seat.Type.STANDARD, bus2);
        seat2C = createSeat("2B", Seat.Type.PREFERENTIAL, bus2);
    }
    private Bus createBus(String plate, Integer capacity, Bus.Status status) {
        Bus bus = Bus.builder()
                .plate(plate)
                .capacity(capacity)
                .status(status)
                .amenities(List.of("WiFi", "AC"))
                .build();
        return busRepository.save(bus);
    }

    private Seat createSeat(String number, Seat.Type type, Bus bus) {
        Seat seat = Seat.builder()
                .number(number)
                .type(type)
                .bus(bus)
                .build();
        return seatRepository.save(seat);
    }

    @Test
    @DisplayName("Debe encontrar todos los asientos de un bus ordenados por número")
    void findByBusIdOrderByNumberAsc_ShouldReturnSeatsOrderedByNumber() {
        // When
        List<Seat> seats = seatRepository.findByBusIdOrderByNumberAsc(bus2.getId());

        // Then
        assertThat(seats)
                .hasSize(3)
                .extracting(Seat::getNumber)
                .containsExactly("1A", "2A", "2B");
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el bus no tiene asientos")
    void findByBusIdOrderByNumberAsc_ShouldReturnEmptyList_WhenBusHasNoSeats() {
        // Given
        Bus emptyBus = createBus("EMPTY001", 20, Bus.Status.ACTIVE);

        // When
        List<Seat> seats = seatRepository.findByBusIdOrderByNumberAsc(emptyBus.getId());

        // Then
        assertThat(seats).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el bus no existe")
    void findByBusIdOrderByNumberAsc_ShouldReturnEmptyList_WhenBusDoesNotExist() {
        // When
        List<Seat> seats = seatRepository.findByBusIdOrderByNumberAsc(999L);

        // Then
        assertThat(seats).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar un asiento específico por bus y número")
    void findByBusIdAndNumber_ShouldReturnSeat_WhenExists() {
        // When
        Optional<Seat> result = seatRepository.findByBusIdAndNumber(bus1.getId(), "1A");

        // Then
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(seat -> {
                    assertThat(seat.getNumber()).isEqualTo("1A");
                    assertThat(seat.getBus().getId()).isEqualTo(bus1.getId());
                    assertThat(seat.getType()).isEqualTo(Seat.Type.STANDARD);
                });
    }

    @Test
    @DisplayName("Debe retornar vacío si el número de asiento no existe en el bus")
    void findByBusIdAndNumber_ShouldReturnEmpty_WhenSeatNumberDoesNotExist() {
        // When
        Optional<Seat> result = seatRepository.findByBusIdAndNumber(bus1.getId(), "99Z");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar vacío si el bus no existe")
    void findByBusIdAndNumber_ShouldReturnEmpty_WhenBusDoesNotExist() {
        // When
        Optional<Seat> result = seatRepository.findByBusIdAndNumber(999L, "1A");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe distinguir asientos con mismo número en diferentes buses")
    void findByBusIdAndNumber_ShouldDistinguishSeatsBetweenBuses() {
        // When - Buscar "1A" en bus1
        Optional<Seat> bus1Seat = seatRepository.findByBusIdAndNumber(bus1.getId(), "1A");

        // When - Buscar "1A" en bus2
        Optional<Seat> bus2Seat = seatRepository.findByBusIdAndNumber(bus2.getId(), "1A");

        // Then - Ambos existen pero son diferentes entidades
        assertThat(bus1Seat).isPresent();
        assertThat(bus2Seat).isPresent();
        assertThat(bus1Seat.get().getId()).isNotEqualTo(bus2Seat.get().getId());
        assertThat(bus1Seat.get().getBus().getId()).isEqualTo(bus1.getId());
        assertThat(bus2Seat.get().getBus().getId()).isEqualTo(bus2.getId());
    }

    @Test
    @DisplayName("Debe encontrar todos los asientos STANDARD de un bus")
    void findByBusIdAndType_ShouldReturnStandardSeats() {
        // When
        List<Seat> standardSeats = seatRepository.findByBusIdAndType(bus2.getId(), Seat.Type.STANDARD);

        // Then
        assertThat(standardSeats)
                .hasSize(2)
                .extracting(Seat::getNumber)
                .containsExactlyInAnyOrder("1A", "2A");
    }

    @Test
    @DisplayName("Debe encontrar todos los asientos PREFERENTIAL de un bus")
    void findByBusIdAndType_ShouldReturnPreferentialSeats() {
        // When
        List<Seat> preferentialSeats = seatRepository.findByBusIdAndType(bus2.getId(), Seat.Type.PREFERENTIAL);

        // Then
        assertThat(preferentialSeats)
                .hasSize(1)
                .extracting(Seat::getNumber)
                .containsExactly("2B");
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay asientos del tipo solicitado")
    void findByBusIdAndType_ShouldReturnEmptyList_WhenNoSeatsOfType() {
        // Given - Bus1 solo tiene 1 STANDARD y 1 PREFERENTIAL
        // When - Buscar todos PREFERENTIAL (solo hay 1)
        List<Seat> preferentialSeats = seatRepository.findByBusIdAndType(bus1.getId(), Seat.Type.PREFERENTIAL);

        // Then
        assertThat(preferentialSeats).hasSize(1);

        // Given - Crear bus sin asientos PREFERENTIAL
        Bus bus3 = createBus("BUS333", 20, Bus.Status.ACTIVE);
        createSeat("1A", Seat.Type.STANDARD, bus3);

        // When
        List<Seat> noPrefSeats = seatRepository.findByBusIdAndType(bus3.getId(), Seat.Type.PREFERENTIAL);

        // Then
        assertThat(noPrefSeats).isEmpty();
    }

    @Test
    @DisplayName("Debe contar correctamente los asientos de un bus")
    void countByBusId_ShouldReturnCorrectCount() {
        // When
        long bus1Count = seatRepository.countByBusId(bus1.getId());
        long bus2Count = seatRepository.countByBusId(bus2.getId());

        // Then
        assertThat(bus1Count).isEqualTo(2);
        assertThat(bus2Count).isEqualTo(3);
    }

    @Test
    @DisplayName("Debe retornar 0 si el bus no tiene asientos")
    void countByBusId_ShouldReturnZero_WhenBusHasNoSeats() {
        // Given
        Bus emptyBus = createBus("EMPTY002", 20, Bus.Status.ACTIVE);

        // When
        long count = seatRepository.countByBusId(emptyBus.getId());

        // Then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Debe retornar 0 si el bus no existe")
    void countByBusId_ShouldReturnZero_WhenBusDoesNotExist() {
        // When
        long count = seatRepository.countByBusId(999L);

        // Then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("No debe permitir asientos duplicados (mismo número en mismo bus)")
    void shouldNotAllowDuplicateSeatsInSameBus() {
        // Given - Ya existe seat "1A" en bus1

        // When - Intentar crear otro "1A" en bus1
        Seat duplicateSeat = Seat.builder()
                .number("1A")
                .type(Seat.Type.STANDARD)
                .bus(bus1)
                .build();

        // Then - Debe lanzar excepción de constraint
        assertThatThrownBy(() -> {
            seatRepository.saveAndFlush(duplicateSeat);
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException o similar
    }

    @Test
    @DisplayName("Debe permitir mismo número de asiento en diferentes buses")
    void shouldAllowSameSeatNumberInDifferentBuses() {
        // When - Ambos buses ya tienen asiento "1A"
        Optional<Seat> bus1Seat1A = seatRepository.findByBusIdAndNumber(bus1.getId(), "1A");
        Optional<Seat> bus2Seat1A = seatRepository.findByBusIdAndNumber(bus2.getId(), "1A");

        // Then - Ambos existen y son diferentes
        assertThat(bus1Seat1A).isPresent();
        assertThat(bus2Seat1A).isPresent();
        assertThat(bus1Seat1A.get().getId()).isNotEqualTo(bus2Seat1A.get().getId());
    }

}