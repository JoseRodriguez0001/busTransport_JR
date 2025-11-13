package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.api.dto.ParcelDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.ParcelRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.services.IncidentService;
import com.unimag.bustransport.services.mapper.ParcelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Unit Tests")
class ParcelServiceImplTest {

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private IncidentService incidentService;

    private final ParcelMapper parcelMapper = Mappers.getMapper(ParcelMapper.class);

    private ParcelServiceImpl parcelService;

    @BeforeEach
    void setUp() {
        parcelService = new ParcelServiceImpl(
                parcelRepository,
                stopRepository,
                tripRepository,
                incidentService,
                parcelMapper
        );
    }

    private Route givenRoute() {
        return Route.builder()
                .id(1L)
                .code("R001")
                .origin("City A")
                .destination("City B")
                .build();
    }

    private Stop givenStop(Long id, String name, Integer order, Route route) {
        return Stop.builder()
                .id(id)
                .name(name)
                .order(order)
                .route(route)
                .build();
    }

    private Trip givenTrip(Route route, Trip.Status status) {
        return Trip.builder()
                .id(1L)
                .route(route)
                .date(LocalDate.now())
                .status(status)
                .build();
    }

    private Parcel givenParcel(Stop fromStop, Stop toStop, Parcel.Status status) {
        return Parcel.builder()
                .id(1L)
                .code("PAQ-20251112-0001")
                .price(BigDecimal.valueOf(50000))
                .status(status)
                .senderName("John Doe")
                .senderPhone("3001234567")
                .receiverName("Jane Smith")
                .receiverPhone("3009876543")
                .fromStop(fromStop)
                .toStop(toStop)
                .deliveryOtp("123456")
                .build();
    }

    private ParcelDtos.ParcelCreateRequest givenCreateRequest(Long fromStopId, Long toStopId, Long tripId) {
        return new ParcelDtos.ParcelCreateRequest(
                BigDecimal.valueOf(50000),
                "John Doe",
                "3001234567",
                "Jane Smith",
                "3009876543",
                fromStopId,
                toStopId,
                tripId
        );
    }

    private ParcelDtos.ParcelUpdateRequest givenUpdateRequest() {
        return new ParcelDtos.ParcelUpdateRequest(
                "Updated Sender",
                "3001111111",
                "Updated Receiver",
                "3009999999",
                BigDecimal.valueOf(60000),
                null,
                null
        );
    }

    @Test
    @DisplayName("Debe crear parcel sin trip correctamente")
    void shouldCreateParcelWithoutTrip() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        ParcelDtos.ParcelCreateRequest request = givenCreateRequest(1L, 2L, null);
        Parcel savedParcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);

        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(parcelRepository.existsByCode(anyString())).thenReturn(false);
        when(parcelRepository.save(any(Parcel.class))).thenReturn(savedParcel);

        // When
        ParcelDtos.ParcelResponse response = parcelService.createParcel(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("CREATED");

        verify(stopRepository, times(1)).findById(1L);
        verify(stopRepository, times(1)).findById(2L);
        verify(parcelRepository, times(1)).save(any(Parcel.class));
        verify(tripRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Debe crear parcel con trip y marcar como IN_TRANSIT")
    void shouldCreateParcelWithTripAndMarkInTransit() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Trip trip = givenTrip(route, Trip.Status.SCHEDULED);
        ParcelDtos.ParcelCreateRequest request = givenCreateRequest(1L, 2L, 1L);
        Parcel savedParcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);

        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(parcelRepository.existsByCode(anyString())).thenReturn(false);
        when(parcelRepository.save(any(Parcel.class))).thenReturn(savedParcel);

        // When
        ParcelDtos.ParcelResponse response = parcelService.createParcel(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("IN_TRANSIT");

        verify(tripRepository, times(1)).findById(1L);
        verify(parcelRepository, times(1)).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando stops no pertenecen a la misma ruta")
    void shouldThrowExceptionWhenStopsNotInSameRoute() {
        // Given
        Route route1 = givenRoute();
        Route route2 = Route.builder().id(2L).build();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route1);
        Stop toStop = givenStop(2L, "Stop 2", 2, route2);
        ParcelDtos.ParcelCreateRequest request = givenCreateRequest(1L, 2L, null);

        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));

        // When & Then
        assertThatThrownBy(() -> parcelService.createParcel(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin and destination stops must belong to the same route");

        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando orden de stops es inválido")
    void shouldThrowExceptionWhenStopOrderInvalid() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 2, route);
        Stop toStop = givenStop(2L, "Stop 2", 1, route);
        ParcelDtos.ParcelCreateRequest request = givenCreateRequest(1L, 2L, null);

        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));

        // When & Then
        assertThatThrownBy(() -> parcelService.createParcel(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin stop order must be less than destination stop order");

        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe actualizar parcel correctamente cuando está en CREATED")
    void shouldUpdateParcelWhenCreated() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel existingParcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);
        ParcelDtos.ParcelUpdateRequest request = givenUpdateRequest();

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(existingParcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(existingParcel);

        // When
        parcelService.updateParcel(1L, request);

        // Then
        verify(parcelRepository, times(1)).findById(1L);
        verify(parcelRepository, times(1)).save(existingParcel);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar parcel que no está en CREATED")
    void shouldThrowExceptionWhenUpdatingNonCreatedParcel() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel existingParcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);
        ParcelDtos.ParcelUpdateRequest request = givenUpdateRequest();

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(existingParcel));

        // When & Then
        assertThatThrownBy(() -> parcelService.updateParcel(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only update parcels with status CREATED");

        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe asignar trip a parcel correctamente")
    void shouldAssignTripToParcel() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);
        Trip trip = givenTrip(route, Trip.Status.SCHEDULED);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);

        // When
        parcelService.assignTrip(1L, 1L);

        // Then
        verify(parcelRepository, times(1)).findById(1L);
        verify(tripRepository, times(1)).findById(1L);
        verify(parcelRepository, times(1)).save(parcel);
    }

    @Test
    @DisplayName("Debe lanzar excepción al asignar trip a parcel que no está en CREATED")
    void shouldThrowExceptionWhenAssigningTripToNonCreatedParcel() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));

        // When & Then
        assertThatThrownBy(() -> parcelService.assignTrip(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only assign trip to parcels with status CREATED");

        verify(tripRepository, never()).findById(anyLong());
        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe obtener parcels por sender phone")
    void shouldGetParcelsBySenderPhone() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        List<Parcel> parcels = List.of(givenParcel(fromStop, toStop, Parcel.Status.CREATED));

        when(parcelRepository.findBySenderPhone("3001234567")).thenReturn(parcels);

        // When
        List<ParcelDtos.ParcelResponse> result = parcelService.getParcelsBySender("3001234567");

        // Then
        assertThat(result).hasSize(1);
        verify(parcelRepository, times(1)).findBySenderPhone("3001234567");
    }

    @Test
    @DisplayName("Debe obtener parcel por código")
    void shouldGetParcelByCode() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);

        when(parcelRepository.findByCode("PAQ-20251112-0001")).thenReturn(Optional.of(parcel));

        // When
        ParcelDtos.ParcelResponse response = parcelService.getParcelByCode("PAQ-20251112-0001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("PAQ-20251112-0001");

        verify(parcelRepository, times(1)).findByCode("PAQ-20251112-0001");
    }

    @Test
    @DisplayName("Debe obtener parcels por trip ID")
    void shouldGetParcelsByTrip() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        List<Parcel> parcels = List.of(givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT));

        when(tripRepository.existsById(1L)).thenReturn(true);
        when(parcelRepository.findByTripId(1L)).thenReturn(parcels);

        // When
        List<ParcelDtos.ParcelResponse> result = parcelService.getParcelsByTrip(1L);

        // Then
        assertThat(result).hasSize(1);
        verify(tripRepository, times(1)).existsById(1L);
        verify(parcelRepository, times(1)).findByTripId(1L);
    }

    @Test
    @DisplayName("Debe confirmar entrega correctamente con OTP válido")
    void shouldConfirmDeliveryWithValidOtp() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);

        // When
        parcelService.confirmDelivery(1L, "123456", "https://photo.url");

        // Then
        verify(parcelRepository, times(1)).findById(1L);
        verify(parcelRepository, times(1)).save(parcel);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando parcel no está IN_TRANSIT para confirmar entrega")
    void shouldThrowExceptionWhenConfirmingDeliveryForNonInTransitParcel() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));

        // When & Then
        assertThatThrownBy(() -> parcelService.confirmDelivery(1L, "123456", "https://photo.url"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Parcel must be in transit to confirm delivery");

        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("Debe marcar parcel como FAILED y crear incident")
    void shouldMarkParcelAsFailedAndCreateIncident() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);
        when(incidentService.createIncident(any(IncidentDtos.IncidentCreateRequest.class)))
                .thenReturn(null);

        // When
        parcelService.markAsFailed(1L, "Lost in transit");

        // Then
        verify(parcelRepository, times(1)).findById(1L);
        verify(parcelRepository, times(1)).save(parcel);
        verify(incidentService, times(1)).createIncident(any(IncidentDtos.IncidentCreateRequest.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al marcar como FAILED parcel que no está IN_TRANSIT")
    void shouldThrowExceptionWhenMarkingAsFailedNonInTransitParcel() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.CREATED);

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));

        // When & Then
        assertThatThrownBy(() -> parcelService.markAsFailed(1L, "Test reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only mark IN_TRANSIT parcels as failed");

        verify(parcelRepository, never()).save(any(Parcel.class));
        verify(incidentService, never()).createIncident(any());
    }

    @Test
    @DisplayName("Debe marcar parcel como FAILED cuando OTP es incorrecto")
    void shouldMarkAsFailedWhenOtpIsIncorrect() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        Parcel parcel = givenParcel(fromStop, toStop, Parcel.Status.IN_TRANSIT);
        parcel.setDeliveryOtp("123456"); // OTP correcto

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);
        when(incidentService.createIncident(any(IncidentDtos.IncidentCreateRequest.class)))
                .thenReturn(null);

        // When
        parcelService.confirmDelivery(1L, "999999", "https://photo.url"); // OTP incorrecto

        // Then
        verify(parcelRepository, times(2)).findById(1L); // 1 en confirmDelivery, 1 en markAsFailed
        verify(parcelRepository, times(1)).save(parcel);
        verify(incidentService, times(1)).createIncident(any(IncidentDtos.IncidentCreateRequest.class));
    }
}