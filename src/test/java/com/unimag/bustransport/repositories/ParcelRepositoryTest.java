package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ParcelRepositoryTest extends AbstractRepositoryTI{
    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private BusRepository busRepository;

    @BeforeEach
    void setUp() {
        parcelRepository.deleteAll();
        tripRepository.deleteAll();
        stopRepository.deleteAll();
        routeRepository.deleteAll();
        busRepository.deleteAll();
    }

    private Stop givenStop(Route route, String name, Integer order) {
        Stop stop = Stop.builder()
                .name(name)
                .order(order)
                .route(route)
                .build();
        return stopRepository.save(stop);
    }

    private Trip givenTrip() {
        Route route = Route.builder()
                .code("R001")
                .origin("A")
                .destination("B")
                .distanceKm(100.0)
                .durationMin(120)
                .build();
        routeRepository.save(route);

        Bus bus = Bus.builder()
                .plate("ABC123")
                .capacity(40)
                .status(Bus.Status.ACTIVE)
                .build();
        busRepository.save(bus);

        Trip trip = Trip.builder()
                .route(route)
                .bus(bus)
                .date(LocalDate.now())
                .departureAt(OffsetDateTime.now())
                .arrivalAt(OffsetDateTime.now().plusHours(2))
                .build();
        return tripRepository.save(trip);
    }

    @Test
    @DisplayName("Debe encontrar parcel por código")
    void shouldFindParcelByCode() {
        // Given
        Trip trip = givenTrip();
        Stop fromStop = givenStop(trip.getRoute(), "Stop A", 1);
        Stop toStop = givenStop(trip.getRoute(), "Stop B", 2);

        Parcel parcel = Parcel.builder()
                .code("PCL-001")
                .price(BigDecimal.valueOf(20000))
                .status(Parcel.Status.CREATED)
                .senderName("Esteban Puello")
                .senderPhone("3001234567")
                .receiverName("Jose Rodriguez")
                .receiverPhone("3009876543")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel);

        // When
        Optional<Parcel> found = parcelRepository.findByCode("PCL-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSenderName()).isEqualTo("Esteban Puello");
        assertThat(found.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    @DisplayName("Debe encontrar parcels por trip ID")
    void shouldFindParcelsByTripId() {
        // Given
        Trip trip = givenTrip();
        Stop fromStop = givenStop(trip.getRoute(), "Stop A", 1);
        Stop toStop = givenStop(trip.getRoute(), "Stop B", 2);

        Parcel parcel1 = Parcel.builder()
                .code("PCL-001")
                .price(BigDecimal.valueOf(20000))
                .status(Parcel.Status.CREATED)
                .senderName("Sender 1")
                .receiverName("Receiver 1")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel1);

        Parcel parcel2 = Parcel.builder()
                .code("PCL-002")
                .price(BigDecimal.valueOf(30000))
                .status(Parcel.Status.IN_TRANSIT)
                .senderName("Sender 2")
                .receiverName("Receiver 2")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel2);

        // When
        List<Parcel> found = parcelRepository.findByTripId(trip.getId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Parcel::getCode)
                .containsExactlyInAnyOrder("PCL-001", "PCL-002");
    }

    @Test
    @DisplayName("Debe encontrar parcels por teléfono del remitente")
    void shouldFindParcelsBySenderPhone() {
        // Given
        Trip trip = givenTrip();
        Stop fromStop = givenStop(trip.getRoute(), "Stop A", 1);
        Stop toStop = givenStop(trip.getRoute(), "Stop B", 2);

        Parcel parcel = Parcel.builder()
                .code("PCL-003")
                .price(BigDecimal.valueOf(25000))
                .status(Parcel.Status.CREATED)
                .senderName("Esteban Puello")
                .senderPhone("3001234567")
                .receiverName("Jose Rodriguez")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel);

        // When
        List<Parcel> found = parcelRepository.findBySenderPhone("3001234567");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSenderName()).isEqualTo("Esteban Puello");
    }

    @Test
    @DisplayName("Debe encontrar parcels por teléfono del destinatario")
    void shouldFindParcelsByReceiverPhone() {
        // Given
        Trip trip = givenTrip();
        Stop fromStop = givenStop(trip.getRoute(), "Stop A", 1);
        Stop toStop = givenStop(trip.getRoute(), "Stop B", 2);

        Parcel parcel = Parcel.builder()
                .code("PCL-004")
                .price(BigDecimal.valueOf(18000))
                .status(Parcel.Status.CREATED)
                .senderName("Esteban Puello")
                .receiverName("Jose Rodriguez")
                .receiverPhone("3009876543")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel);

        // When
        List<Parcel> found = parcelRepository.findByReceiverPhone("3009876543");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getReceiverName()).isEqualTo("Jose Rodriguez");
    }

    @Test
    @DisplayName("Debe verificar si existe código de parcel")
    void shouldCheckIfCodeExists() {
        // Given
        Trip trip = givenTrip();
        Stop fromStop = givenStop(trip.getRoute(), "Stop A", 1);
        Stop toStop = givenStop(trip.getRoute(), "Stop B", 2);

        Parcel parcel = Parcel.builder()
                .code("PCL-UNIQUE")
                .price(BigDecimal.valueOf(15000))
                .status(Parcel.Status.CREATED)
                .senderName("Sender")
                .receiverName("Receiver")
                .trip(trip)
                .fromStop(fromStop)
                .toStop(toStop)
                .build();
        parcelRepository.save(parcel);

        // When
        boolean exists = parcelRepository.existsByCode("PCL-UNIQUE");
        boolean notExists = parcelRepository.existsByCode("PCL-NOTFOUND");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
