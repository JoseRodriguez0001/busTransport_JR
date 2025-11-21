package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.repositories.FareRuleRepository;
import com.unimag.bustransport.domain.repositories.RouteRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FareRuleRepositoryTest extends AbstractRepositoryTI{

    @Autowired
    private FareRuleRepository fareRuleRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private StopRepository stopRepository;

    @BeforeEach
    void setUp() {
        fareRuleRepository.deleteAll();
        stopRepository.deleteAll();
        routeRepository.deleteAll();
    }

    private Route givenRoute() {
        Route route = Route.builder()
                .code("R001")
                .name("Test Route")
                .origin("City A")
                .destination("City B")
                .distanceKm(100.0)
                .durationMin(120)
                .build();
        return routeRepository.save(route);
    }

    private Stop givenStop(Route route, String name, Integer order) {
        Stop stop = Stop.builder()
                .name(name)
                .order(order)
                .route(route)
                .build();
        return stopRepository.save(stop);
    }

    private FareRule givenFareRule(Route route, Stop fromStop, Stop toStop) {
        Map<String, Double> discounts = new HashMap<>();
        discounts.put("child", 0.5);
        discounts.put("senior", 0.3);

        FareRule fareRule = FareRule.builder()
                .route(route)
                .fromStop(fromStop)
                .toStop(toStop)
                .basePrice(BigDecimal.valueOf(50000))
                .discounts(discounts)
                .dynamicPricing(FareRule.DynamicPricing.ON)
                .build();
        return fareRuleRepository.save(fareRule);
    }

    @Test
    @DisplayName("Debe encontrar fare rule por route, fromStop y toStop")
    void shouldFindFareRuleByRouteAndStops() {
        // Given
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Stop 1", 1);
        Stop stop2 = givenStop(route, "Stop 2", 2);
        FareRule fareRule = givenFareRule(route, stop1, stop2);

        // When
        Optional<FareRule> found = fareRuleRepository
                .findByRouteIdAndFromStopIdAndToStopId(route.getId(), stop1.getId(), stop2.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(fareRule.getId());
        assertThat(found.get().getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("Debe encontrar fare rules por route ID")
    void shouldFindFareRulesByRouteId() {
        // Given
        Route route = givenRoute();
        Stop stop1 = givenStop(route, "Stop 1", 1);
        Stop stop2 = givenStop(route, "Stop 2", 2);
        Stop stop3 = givenStop(route, "Stop 3", 3);

        givenFareRule(route, stop1, stop2);
        givenFareRule(route, stop2, stop3);

        // When
        List<FareRule> found = fareRuleRepository.findByRouteId(route.getId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(fr -> fr.getRoute().getId().equals(route.getId()));
    }

    @Test
    @DisplayName("Debe retornar empty cuando no existe combinación de route y stops")
    void shouldReturnEmptyWhenRouteAndStopsNotFound() {
        // When
        Optional<FareRule> found = fareRuleRepository
                .findByRouteIdAndFromStopIdAndToStopId(999L, 888L, 777L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando route no tiene fare rules")
    void shouldReturnEmptyListWhenNoFareRulesForRoute() {
        // Given
        Route route = givenRoute();

        // When
        List<FareRule> found = fareRuleRepository.findByRouteId(route.getId());

        // Then
        assertThat(found).isEmpty();
    }
}
