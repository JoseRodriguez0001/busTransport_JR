package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.domain.repositories.BusRepository;
import com.unimag.bustransport.domain.repositories.SeatRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class BusRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private BusRepository busRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SeatRepository seatRepository;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        busRepository.deleteAll();
    }

    private Bus givenBus(String plate, Integer capacity, Bus.Status status) {
        Bus bus = Bus.builder()
                .plate(plate)
                .capacity(capacity)
                .amenities(new ArrayList<>(List.of("WiFi", "AC")))
                .status(status)
                .build();
        return busRepository.save(bus);
    }

    private Bus givenActiveBus() {
        return givenBus("ABC123", 40, Bus.Status.ACTIVE);
    }

    private Seat givenSeat(Bus bus, String seatNumber) {
        Seat seat = Seat.builder()
                .number(seatNumber)
                .type(Seat.Type.STANDARD)
                .bus(bus)
                .build();
        return seatRepository.save(seat);
    }

    @Test
    @DisplayName("Debe encontrar bus por placa")
    void shouldFindBusByPlate() {
        // Given
        Bus saved = givenBus("XYZ789", 40, Bus.Status.ACTIVE);

        // When
        Optional<Bus> found = busRepository.findByPlate("XYZ789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCapacity()).isEqualTo(40);
    }

    @Test
    @DisplayName("Debe retornar empty cuando la placa no existe")
    void shouldReturnEmptyWhenPlateNotFound() {
        // When
        Optional<Bus> found = busRepository.findByPlate("NOEXISTE");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar buses por status")
    void shouldFindBusesByStatus() {
        // Given
        givenBus("BUS001", 40, Bus.Status.ACTIVE);
        givenBus("BUS002", 40, Bus.Status.ACTIVE);
        givenBus("BUS003", 40, Bus.Status.IN_REPAIR);
        givenBus("BUS004", 40, Bus.Status.RETIRED);

        // When
        List<Bus> activeBuses = busRepository.findByStatus(Bus.Status.ACTIVE);

        // Then
        assertThat(activeBuses).hasSize(2);
        assertThat(activeBuses).allMatch(b -> b.getStatus() == Bus.Status.ACTIVE);
        assertThat(activeBuses).extracting(Bus::getPlate)
                .containsExactlyInAnyOrder("BUS001", "BUS002");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay buses con el status")
    void shouldReturnEmptyListWhenNoStatusMatches() {
        // Given
        givenBus("BUS001", 40, Bus.Status.ACTIVE);

        // When
        List<Bus> retiredBuses = busRepository.findByStatus(Bus.Status.RETIRED);

        // Then
        assertThat(retiredBuses).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar bus con asientos usando JOIN FETCH")
    void shouldFindBusByIdWithSeats() {
        // Given
        Bus bus = givenBus("XYZ789", 40, Bus.Status.ACTIVE);
        givenSeat(bus, "1A");
        givenSeat(bus, "1B");
        givenSeat(bus, "2A");

        entityManager.flush();
        entityManager.clear();
        // When
        Optional<Bus> found = busRepository.findByIdWithSeats(bus.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSeats()).isNotNull();
        assertThat(found.get().getSeats()).hasSize(3);
        assertThat(found.get().getSeats()).extracting(Seat::getNumber)
                .containsExactlyInAnyOrder("1A", "1B", "2A");
    }

    @Test
    @DisplayName("Debe retornar empty cuando bus no existe en findByIdWithSeats")
    void shouldReturnEmptyWhenBusNotFoundWithSeats() {
        // When
        Optional<Bus> found = busRepository.findByIdWithSeats(999L);

        // Then
        assertThat(found).isEmpty();
    }



    @Test
    @DisplayName("Debe cambiar el status del bus")
    void shouldChangeBusStatus() {
        // Given
        Bus bus = givenBus("BUS999", 40, Bus.Status.ACTIVE);

        // When
        busRepository.changeBusStatus(bus.getId(), Bus.Status.IN_REPAIR);
        entityManager.flush();
        entityManager.clear();

        // Then
        Bus updated = busRepository.findById(bus.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Bus.Status.IN_REPAIR);
    }

    @Test
    @DisplayName("Debe cambiar status de ACTIVE a RETIRED")
    void shouldChangeBusStatusFromActiveToRetired() {
        // Given
        Bus bus = givenBus("BUS100", 40, Bus.Status.ACTIVE);

        // When
        busRepository.changeBusStatus(bus.getId(), Bus.Status.RETIRED);
        entityManager.flush();
        entityManager.clear();

        // Then
        Bus updated = busRepository.findById(bus.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Bus.Status.RETIRED);
    }

    @Test
    @DisplayName("Debe cambiar status de IN_REPAIR a ACTIVE")
    void shouldChangeBusStatusFromInRepairToActive() {
        // Given
        Bus bus = givenBus("BUS200", 40, Bus.Status.IN_REPAIR);

        // When
        busRepository.changeBusStatus(bus.getId(), Bus.Status.ACTIVE);
        entityManager.flush();
        entityManager.clear();

        // Then
        Bus updated = busRepository.findById(bus.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Bus.Status.ACTIVE);
    }
}
