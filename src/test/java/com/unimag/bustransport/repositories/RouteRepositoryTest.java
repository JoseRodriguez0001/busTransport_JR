package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RouteRepositoryTest extends AbstractRepositoryTI{

    @Autowired
    private RouteRepository routeRepository;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
    }

    private Route givenRoute(String code, String origin, String destination) {
        Route route = Route.builder()
                .code(code)
                .name(origin + " - " + destination)
                .origin(origin)
                .destination(destination)
                .distanceKm(100.0)
                .durationMin(120)
                .build();
        return routeRepository.save(route);
    }


    @Test
    @DisplayName("Debe encontrar ruta por origen y destino")
    void shouldFindRouteByOriginAndDestination() {
        // Given
        givenRoute("R001", "Barranquilla", "Cartagena");
        givenRoute("R002", "Barranquilla", "Santa Marta");
        givenRoute("R003", "Cartagena", "Santa Marta");

        // When
        List<Route> found = routeRepository.findByOriginAndDestination("Barranquilla", "Cartagena");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCode()).isEqualTo("R001");
        assertThat(found.get(0).getOrigin()).isEqualTo("Barranquilla");
        assertThat(found.get(0).getDestination()).isEqualTo("Cartagena");
    }

    @Test
    @DisplayName("Debe encontrar rutas por origen")
    void shouldFindRoutesByOrigin() {
        // Given
        givenRoute("R001", "Barranquilla", "Cartagena");
        givenRoute("R002", "Barranquilla", "Santa Marta");
        givenRoute("R003", "Cartagena", "Valledupar");

        // When
        List<Route> found = routeRepository.findByOrigin("Barranquilla");

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Route::getCode)
                .containsExactlyInAnyOrder("R001", "R002");
    }

    @Test
    @DisplayName("Debe encontrar rutas por destino")
    void shouldFindRoutesByDestination() {
        // Given
        givenRoute("R001", "Barranquilla", "Santa Marta");
        givenRoute("R002", "Cartagena", "Santa Marta");
        givenRoute("R003", "Valledupar", "Barranquilla");

        // When
        List<Route> found = routeRepository.findByDestination("Santa Marta");

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Route::getCode)
                .containsExactlyInAnyOrder("R001", "R002");
    }

    @Test
    @DisplayName("Debe verificar si existe código de ruta")
    void shouldCheckIfCodeExists() {
        // Given
        givenRoute("R-UNIQUE", "Bogotá", "Medellin");

        // When
        boolean exists = routeRepository.existsByCode("R-UNIQUE");
        boolean notExists = routeRepository.existsByCode("R-NOTFOUND");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }


    @Test
    @DisplayName("Debe retornar lista vacía cuando no existe combinación origen-destino")
    void shouldReturnEmptyListWhenOriginDestinationNotFound() {
        // Given
        givenRoute("R001", "Barranquilla", "Cartagena");

        // When
        List<Route> found = routeRepository.findByOriginAndDestination("Bogotá", "Cali");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay rutas desde origen")
    void shouldReturnEmptyListWhenNoRoutesFromOrigin() {
        // Given
        givenRoute("R001", "Barranquilla", "Cartagena");

        // When
        List<Route> found = routeRepository.findByOrigin("Medellín");

        // Then
        assertThat(found).isEmpty();
    }
}
