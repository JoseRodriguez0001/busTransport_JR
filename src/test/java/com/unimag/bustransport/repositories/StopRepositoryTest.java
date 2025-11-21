package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class StopRepositoryTest extends AbstractRepositoryTI {

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private RouteRepository routeRepository;

    private Route route1;
    private Route route2;
    private Stop stop1;
    private Stop stop2;
    private Stop stop3;
    private Stop stop4;
    private Stop stop5;

    @BeforeEach
    void setUp() {
        stopRepository.deleteAll();
        routeRepository.deleteAll();

        // Crear rutas
        route1 = createRoute("R001", "Santa Marta", "Barranquilla", 100.0);
        route2 = createRoute("R002", "Cartagena", "Medellín", 500.0);

        stop1 = createStop("Terminal Santa Marta", 0, 11.2408, -74.1990, route1);
        stop2 = createStop("Ciénaga", 1, 11.0061, -74.2466, route1);
        stop3 = createStop("Fundación", 2, 10.5206, -74.1852, route1);
        stop4 = createStop("Terminal Barranquilla", 3, 10.9878, -74.8148, route1);

        // Crear parada para route2
        stop5 = createStop("Terminal Cartagena", 0, 10.3910, -75.4794, route2);
    }

    private Route createRoute(String code, String origin, String destination, Double distanceKm) {
        Route route = Route.builder()
                .code(code)
                .name(origin + " - " + destination)
                .origin(origin)
                .destination(destination)
                .distanceKm(distanceKm)
                .durationMin(120)
                .build();
        return routeRepository.save(route);
    }

    private Stop createStop(String name, Integer order, Double lat, Double lng, Route route) {
        Stop stop = Stop.builder()
                .name(name)
                .order(order)
                .lat(lat)
                .lng(lng)
                .route(route)
                .build();
        return stopRepository.save(stop);
    }

    @Test
    @DisplayName("Debe encontrar todas las paradas de una ruta ordenadas por order")
    void findByRouteIdOrderByOrderAsc_ShouldReturnStopsOrderedByOrder() {
        // When
        List<Stop> stops = stopRepository.findByRouteIdOrderByOrderAsc(route1.getId());

        // Then
        assertThat(stops)
                .hasSize(4)
                .extracting(Stop::getOrder)
                .containsExactly(0, 1, 2, 3); // Orden ascendente
        
        assertThat(stops)
                .extracting(Stop::getName)
                .containsExactly(
                    "Terminal Santa Marta",
                    "Ciénaga",
                    "Fundación",
                    "Terminal Barranquilla"
                );
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la ruta no tiene paradas")
    void findByRouteIdOrderByOrderAsc_ShouldReturnEmptyList_WhenRouteHasNoStops() {
        // Given
        Route emptyRoute = createRoute("R999", "Origen", "Destino", 200.0);

        // When
        List<Stop> stops = stopRepository.findByRouteIdOrderByOrderAsc(emptyRoute.getId());

        // Then
        assertThat(stops).isEmpty();
    }


    @Test
    @DisplayName("Debe encontrar una parada específica por ruta y order")
    void findByRouteIdAndOrder_ShouldReturnStop_WhenExists() {
        // When
        Optional<Stop> result = stopRepository.findByRouteIdAndOrder(route1.getId(), 2);

        // Then
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(stop -> {
                    assertThat(stop.getName()).isEqualTo("Fundación");
                    assertThat(stop.getOrder()).isEqualTo(2);
                    assertThat(stop.getRoute().getId()).isEqualTo(route1.getId());
                });
    }

    @Test
    @DisplayName("Debe retornar vacío si el order no existe en la ruta")
    void findByRouteIdAndOrder_ShouldReturnEmpty_WhenOrderDoesNotExist() {
        // When
        Optional<Stop> result = stopRepository.findByRouteIdAndOrder(route1.getId(), 999);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe distinguir paradas con mismo order en diferentes rutas")
    void findByRouteIdAndOrder_ShouldDistinguishStopsBetweenRoutes() {
        // When - Buscar order 0 en ambas rutas
        Optional<Stop> route1Stop = stopRepository.findByRouteIdAndOrder(route1.getId(), 0);
        Optional<Stop> route2Stop = stopRepository.findByRouteIdAndOrder(route2.getId(), 0);

        // Then - Ambos existen pero son diferentes
        assertThat(route1Stop).isPresent();
        assertThat(route2Stop).isPresent();
        assertThat(route1Stop.get().getId()).isNotEqualTo(route2Stop.get().getId());
        assertThat(route1Stop.get().getName()).isEqualTo("Terminal Santa Marta");
        assertThat(route2Stop.get().getName()).isEqualTo("Terminal Cartagena");
    }

    @Test
    @DisplayName("Debe retornar true si la parada pertenece a la ruta")
    void existsByRouteIdAndId_ShouldReturnTrue_WhenStopBelongsToRoute() {
        // When
        boolean exists = stopRepository.existsByRouteIdAndId(route1.getId(), stop2.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false si la parada no pertenece a la ruta")
    void existsByRouteIdAndId_ShouldReturnFalse_WhenStopDoesNotBelongToRoute() {
        // When - stop5 pertenece a route2, no a route1
        boolean exists = stopRepository.existsByRouteIdAndId(route1.getId(), stop5.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Debe retornar false si la parada no existe")
    void existsByRouteIdAndId_ShouldReturnFalse_WhenStopDoesNotExist() {
        // When
        boolean exists = stopRepository.existsByRouteIdAndId(route1.getId(), 999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Debe encontrar paradas entre dos órdenes (inclusive)")
    void findStopsBetween_ShouldReturnStopsBetweenOrders() {
        // When - Buscar paradas entre orden 1 y 3 (Ciénaga hasta Terminal Barranquilla)
        List<Stop> stops = stopRepository.findStopsBetween(route1.getId(), 1, 3);

        // Then
        assertThat(stops)
                .hasSize(3)
                .extracting(Stop::getOrder)
                .containsExactly(1, 2, 3);
        
        assertThat(stops)
                .extracting(Stop::getName)
                .containsExactly("Ciénaga", "Fundación", "Terminal Barranquilla");
    }

    @Test
    @DisplayName("Debe encontrar solo la parada inicial si fromOrder = toOrder")
    void findStopsBetween_ShouldReturnSingleStop_WhenFromEqualsTo() {
        // When
        List<Stop> stops = stopRepository.findStopsBetween(route1.getId(), 2, 2);

        // Then
        assertThat(stops)
                .hasSize(1)
                .extracting(Stop::getName)
                .containsExactly("Fundación");
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay paradas en el rango")
    void findStopsBetween_ShouldReturnEmptyList_WhenNoStopsInRange() {
        // When - Buscar paradas entre orden 100 y 200 (no existen)
        List<Stop> stops = stopRepository.findStopsBetween(route1.getId(), 100, 200);

        // Then
        assertThat(stops).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar todas las paradas si el rango las incluye todas")
    void findStopsBetween_ShouldReturnAllStops_WhenRangeIncludesAll() {
        // When - Buscar desde 0 hasta 999
        List<Stop> stops = stopRepository.findStopsBetween(route1.getId(), 0, 999);

        // Then
        assertThat(stops).hasSize(4);
    }

    @Test
    @DisplayName("No debe permitir paradas duplicadas (mismo order en misma ruta)")
    void shouldNotAllowDuplicateStopsInSameRoute() {
        // Given - Ya existe parada con order 1 en route1
        
        // When - Intentar crear otra parada con order 1 en route1
        Stop duplicateStop = Stop.builder()
                .name("Otra parada")
                .order(1)
                .lat(10.0)
                .lng(-75.0)
                .route(route1)
                .build();

        // Then - Debe lanzar excepción de constraint
        assertThatThrownBy(() -> {
            stopRepository.saveAndFlush(duplicateStop);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Debe permitir mismo order en diferentes rutas")
    void shouldAllowSameOrderInDifferentRoutes() {
        // When - Ambas rutas tienen parada con order 0
        Optional<Stop> route1Stop = stopRepository.findByRouteIdAndOrder(route1.getId(), 0);
        Optional<Stop> route2Stop = stopRepository.findByRouteIdAndOrder(route2.getId(), 0);

        // Then - Ambos existen y son diferentes
        assertThat(route1Stop).isPresent();
        assertThat(route2Stop).isPresent();
        assertThat(route1Stop.get().getId()).isNotEqualTo(route2Stop.get().getId());
    }


}