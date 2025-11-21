package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.services.mapper.StopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class StopServiceImplTest {

    @Mock
    private StopRepository stopRepository;

    @Mock
    private RouteRepository routeRepository;
    @Spy
    private StopMapper stopMapper = Mappers.getMapper(StopMapper.class);
    @InjectMocks
    private StopServiceImpl stopService;

    private Route route;
    private Stop stop;
    private StopDtos.StopCreateRequest createRequest;
    private StopDtos.StopUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {

        route = createRoute(1L, "R001", "Santa Marta", "Barranquilla");
        stop = createStop(1L, "Terminal Santa Marta", 0, 11.2408, -74.1990, route);
        
        createRequest = new StopDtos.StopCreateRequest(
                "Ciénaga",
                1,
                11.0061,
                -74.2466,
                1L
        );
        
        updateRequest = new StopDtos.StopUpdateRequest(
                "Ciénaga - Actualizado",
                2,
                11.0061,
                -74.2466
        );
    }

    @Test
    @DisplayName("Debe crear una parada exitosamente")
    void createStop_ShouldCreateSuccessfully() {
        // Given
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(stopRepository.findByRouteIdAndOrder(1L, 1)).thenReturn(Optional.empty());
        when(stopRepository.save(any(Stop.class))).thenAnswer(invocation -> {
            Stop saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // When
        StopDtos.StopResponse response = stopService.createStop(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Ciénaga");
        assertThat(response.order()).isEqualTo(1);
        assertThat(response.lat()).isEqualTo(11.0061);
        assertThat(response.lng()).isEqualTo(-74.2466);

        verify(routeRepository).findById(1L);
        verify(stopRepository).findByRouteIdAndOrder(1L, 1);
        verify(stopRepository).save(any(Stop.class));
    }


    @Test
    @DisplayName("Debe actualizar una parada exitosamente")
    void updateStop_ShouldUpdateSuccessfully() {
        // Given
        when(stopRepository.findById(1L)).thenReturn(Optional.of(stop));
        when(stopRepository.save(any(Stop.class))).thenReturn(stop);

        // When
        stopService.updateStop(1L, updateRequest);

        // Then
        verify(stopRepository).findById(1L);
        verify(stopRepository).save(any(Stop.class));
        assertThat(stop.getName()).isEqualTo("Ciénaga - Actualizado");
        assertThat(stop.getOrder()).isEqualTo(2);
    }


    @Test
    @DisplayName("Debe lanzar excepción si cambia order a uno ya existente")
    void updateStop_ShouldThrowException_WhenChangingToExistingOrder() {
        // Given
        Stop existingStop = createStop(2L, "Otra parada", 2, 10.0, -75.0, route);
        when(stopRepository.findById(1L)).thenReturn(Optional.of(stop));
        when(stopRepository.findByRouteIdAndOrder(route.getId(), 2))
                .thenReturn(Optional.of(existingStop));

        // When & Then
        assertThatThrownBy(() -> stopService.updateStop(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Stop with id 2 already exists");

        verify(stopRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar una parada exitosamente")
    void deleteStop_ShouldDeleteSuccessfully() {
        // Given
        when(stopRepository.findById(1L)).thenReturn(Optional.of(stop));

        // When
        stopService.deleteStop(1L);

        // Then
        verify(stopRepository).findById(1L);
        verify(stopRepository).delete(stop);
    }


    @Test
    @DisplayName("Debe obtener todas las paradas de una ruta")
    void getStopsByRouteId_ShouldReturnAllStops() {
        // Given
        Stop stop2 = createStop(2L, "Ciénaga", 1, 11.0, -74.2, route);
        Stop stop3 = createStop(3L, "Fundación", 2, 10.5, -74.1, route);
        
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(stopRepository.findByRouteIdOrderByOrderAsc(1L))
                .thenReturn(List.of(stop, stop2, stop3));

        // When
        List<StopDtos.StopResponse> responses = stopService.getStopsByRouteId(1L);

        // Then
        assertThat(responses)
                .hasSize(3)
                .extracting(StopDtos.StopResponse::order)
                .containsExactly(0, 1, 2);

        verify(routeRepository).existsById(1L);
        verify(stopRepository).findByRouteIdOrderByOrderAsc(1L);
    }



    @Test
    @DisplayName("Debe obtener una parada por ID")
    void getStopById_ShouldReturnStop() {
        // Given
        when(stopRepository.findById(1L)).thenReturn(Optional.of(stop));

        // When
        StopDtos.StopResponse response = stopService.getStopById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Terminal Santa Marta");

        verify(stopRepository).findById(1L);
    }



    @Test
    @DisplayName("Debe obtener paradas entre dos órdenes")
    void getStopsBetween_ShouldReturnStopsBetweenOrders() {
        // Given
        Stop stop2 = createStop(2L, "Ciénaga", 1, 11.0, -74.2, route);
        Stop stop3 = createStop(3L, "Fundación", 2, 10.5, -74.1, route);
        
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(stopRepository.findStopsBetween(1L, 0, 2))
                .thenReturn(List.of(stop, stop2, stop3));

        // When
        List<StopDtos.StopResponse> responses = stopService.getStopsBetween(1L, 0, 2);

        // Then
        assertThat(responses)
                .hasSize(3)
                .extracting(StopDtos.StopResponse::order)
                .containsExactly(0, 1, 2);

        verify(stopRepository).findStopsBetween(1L, 0, 2);
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

    private Stop createStop(Long id, String name, Integer order, Double lat, Double lng, Route route) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        stop.setOrder(order);
        stop.setLat(lat);
        stop.setLng(lng);
        stop.setRoute(route);
        return stop;
    }
}