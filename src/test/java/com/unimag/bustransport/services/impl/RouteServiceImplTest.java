package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.RouteDtos;
import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.RouteMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private StopRepository stopRepository;
    @Spy
    private final RouteMapper routeMapper = Mappers.getMapper(RouteMapper.class);
    @Spy
    private final StopMapper stopMapper = Mappers.getMapper(StopMapper.class);
    @InjectMocks
    private RouteServiceImpl routeService;

    private Route givenRoute() {
        return Route.builder()
                .id(1L)
                .code("R001")
                .name("Test Route")
                .origin("City A")
                .destination("City B")
                .distanceKm(100.0)
                .durationMin(120)
                .build();
    }

    private Stop givenStop(Route route, String name, Integer order) {
        return Stop.builder()
                .id(1L)
                .name(name)
                .order(order)
                .route(route)
                .build();
    }

    private RouteDtos.RouteCreateRequest givenCreateRequest() {
        return new RouteDtos.RouteCreateRequest(
                "R001",
                "Test Route",
                "City A",
                "City B",
                100.0,
                120
        );
    }

    private RouteDtos.RouteUpdateRequest givenUpdateRequest() {
        return new RouteDtos.RouteUpdateRequest(
                "Updated Route",
                "City X",
                "City Y",
                150.0,
                180
        );
    }

    @Test
    @DisplayName("Debe crear route correctamente")
    void shouldCreateRoute() {
        // Given
        RouteDtos.RouteCreateRequest request = givenCreateRequest();
        Route savedRoute = givenRoute();

        when(routeRepository.existsByCode("R001")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(savedRoute);

        // When
        RouteDtos.RouteResponse response = routeService.createRoute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("R001");
        assertThat(response.origin()).isEqualTo("City A");

        verify(routeRepository, times(1)).existsByCode("R001");
        verify(routeRepository, times(1)).save(any(Route.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando código ya existe")
    void shouldThrowExceptionWhenCodeAlreadyExists() {
        // Given
        RouteDtos.RouteCreateRequest request = givenCreateRequest();

        when(routeRepository.existsByCode("R001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Route with code already exists");

        verify(routeRepository, times(1)).existsByCode("R001");
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    @DisplayName("Debe actualizar route correctamente")
    void shouldUpdateRoute() {
        // Given
        Route existingRoute = givenRoute();
        RouteDtos.RouteUpdateRequest request = givenUpdateRequest();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        when(routeRepository.save(any(Route.class))).thenReturn(existingRoute);

        // When
        routeService.updateRoute(1L, request);

        // Then
        verify(routeRepository, times(1)).findById(1L);
        verify(routeRepository, times(1)).save(existingRoute);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar route inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentRoute() {
        // Given
        RouteDtos.RouteUpdateRequest request = givenUpdateRequest();

        when(routeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> routeService.updateRoute(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        verify(routeRepository, times(1)).findById(999L);
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    @DisplayName("Debe eliminar route correctamente")
    void shouldDeleteRoute() {
        // Given
        Route route = givenRoute();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        doNothing().when(routeRepository).delete(route);

        // When
        routeService.deleteRoute(1L);

        // Then
        verify(routeRepository, times(1)).findById(1L);
        verify(routeRepository, times(1)).delete(route);
    }

    @Test
    @DisplayName("Debe obtener todas las routes")
    void shouldGetAllRoutes() {
        // Given
        List<Route> routes = List.of(
                givenRoute(),
                givenRoute()
        );

        when(routeRepository.findAll()).thenReturn(routes);

        // When
        List<RouteDtos.RouteResponse> result = routeService.getAllRoutes();

        // Then
        assertThat(result).hasSize(2);

        verify(routeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe obtener route por ID")
    void shouldGetRouteById() {
        // Given
        Route route = givenRoute();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        // When
        RouteDtos.RouteResponse response = routeService.getRouteById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(routeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando route no existe por ID")
    void shouldThrowExceptionWhenRouteNotFoundById() {
        // Given
        when(routeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> routeService.getRouteById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        verify(routeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Debe obtener stops por route ID")
    void shouldGetStopsByRouteId() {
        // Given
        Route route = givenRoute();
        List<Stop> stops = List.of(
                givenStop(route, "Stop 1", 1),
                givenStop(route, "Stop 2", 2)
        );

        when(stopRepository.findByRouteIdOrderByOrderAsc(1L)).thenReturn(stops);

        // When
        List<StopDtos.StopResponse> result = routeService.getStopsByRouteId(1L);

        // Then
        assertThat(result).hasSize(2);

        verify(stopRepository, times(1)).findByRouteIdOrderByOrderAsc(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando route no existe al buscar stops")
    void shouldThrowExceptionWhenRouteNotFoundForStops() {
        // Given
        when(stopRepository.findByRouteIdOrderByOrderAsc(999L)).thenReturn(List.of());
        when(routeRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> routeService.getStopsByRouteId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        verify(stopRepository, times(1)).findByRouteIdOrderByOrderAsc(999L);
        verify(routeRepository, times(1)).existsById(999L);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando route existe pero no tiene stops")
    void shouldReturnEmptyListWhenRouteExistsButHasNoStops() {
        // Given
        when(stopRepository.findByRouteIdOrderByOrderAsc(1L)).thenReturn(List.of());
        when(routeRepository.existsById(1L)).thenReturn(true);

        // When
        List<StopDtos.StopResponse> result = routeService.getStopsByRouteId(1L);

        // Then
        assertThat(result).isEmpty();

        verify(stopRepository, times(1)).findByRouteIdOrderByOrderAsc(1L);
        verify(routeRepository, times(1)).existsById(1L);
    }

    @Test
    @DisplayName("Debe buscar routes por origen y destino")
    void shouldSearchRoutesByOriginAndDestination() {
        // Given
        List<Route> routes = List.of(givenRoute());

        when(routeRepository.findByOriginAndDestination("City A", "City B"))
                .thenReturn(routes);

        // When
        List<RouteDtos.RouteResponse> result =
                routeService.searchRoutes("City A", "City B");

        // Then
        assertThat(result).hasSize(1);

        verify(routeRepository, times(1))
                .findByOriginAndDestination("City A", "City B");
    }

    @Test
    @DisplayName("Debe buscar routes por origen solamente")
    void shouldSearchRoutesByOriginOnly() {
        // Given
        List<Route> routes = List.of(givenRoute());

        when(routeRepository.findByOrigin("City A")).thenReturn(routes);

        // When
        List<RouteDtos.RouteResponse> result = routeService.searchRoutes("City A", null);

        // Then
        assertThat(result).hasSize(1);

        verify(routeRepository, times(1)).findByOrigin("City A");
    }

    @Test
    @DisplayName("Debe buscar routes por destino solamente")
    void shouldSearchRoutesByDestinationOnly() {
        // Given
        List<Route> routes = List.of(givenRoute());

        when(routeRepository.findByDestination("City B")).thenReturn(routes);

        // When
        List<RouteDtos.RouteResponse> result = routeService.searchRoutes(null, "City B");

        // Then
        assertThat(result).hasSize(1);

        verify(routeRepository, times(1)).findByDestination("City B");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando origen y destino son null")
    void shouldThrowExceptionWhenBothOriginAndDestinationNull() {
        // When & Then
        assertThatThrownBy(() -> routeService.searchRoutes(null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No routes available");

        verify(routeRepository, never()).findByOriginAndDestination(any(), any());
        verify(routeRepository, never()).findByOrigin(any());
        verify(routeRepository, never()).findByDestination(any());
    }
}