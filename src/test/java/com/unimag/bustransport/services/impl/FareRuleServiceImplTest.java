package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.FareRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareRuleServiceImplTest {

    @Mock
    private FareRuleRepository fareRuleRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BusRepository busRepository;

    @Spy
    private final FareRuleMapper fareRuleMapper = Mappers.getMapper(FareRuleMapper.class);

    @InjectMocks
    private FareRuleServiceImpl fareRuleService;

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

    private Stop givenStop(Long id, String name, Integer order, Route route) {
        return Stop.builder()
                .id(id)
                .name(name)
                .order(order)
                .route(route)
                .build();
    }

    private FareRule givenFareRule(Route route, Stop fromStop, Stop toStop) {
        Map<String, Double> discounts = new HashMap<>();
        discounts.put("child", 0.5);
        discounts.put("senior", 0.3);
        discounts.put("student", 0.2);

        return FareRule.builder()
                .id(1L)
                .route(route)
                .fromStop(fromStop)
                .toStop(toStop)
                .basePrice(BigDecimal.valueOf(50000))
                .discounts(discounts)
                .dynamicPricing(FareRule.DynamicPricing.ON)
                .build();
    }

    private Passenger givenPassenger(int age) {
        LocalDate birthDate = LocalDate.now().minusYears(age);
        return Passenger.builder()
                .id(1L)
                .fullName("Test Passenger")
                .documentType("CC")
                .documentNumber("123456")
                .birthDate(birthDate)
                .phoneNumber("3001234567")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Seat givenSeat(Seat.Type type) {
        return Seat.builder()
                .id(1L)
                .number("A1")
                .type(type)
                .build();
    }

    private Bus givenBus(Integer capacity) {
        return Bus.builder()
                .id(1L)
                .plate("ABC123")
                .capacity(capacity)
                .status(Bus.Status.ACTIVE)
                .build();
    }

    private FareRuleDtos.FareRuleCreateRequest givenCreateRequest() {
        Map<String, Double> discounts = new HashMap<>();
        discounts.put("child", 0.5);

        return new FareRuleDtos.FareRuleCreateRequest(
                1L,  // routeId
                1L,  // fromStopId
                2L,  // toStopId
                BigDecimal.valueOf(50000),
                discounts,
                FareRule.DynamicPricing.ON
        );
    }

    private FareRuleDtos.FareRuleUpdateRequest givenUpdateRequest() {
        return new FareRuleDtos.FareRuleUpdateRequest(
                BigDecimal.valueOf(60000),
                null,
                FareRule.DynamicPricing.OFF
        );
    }

    @Test
    @DisplayName("Debe crear fare rule correctamente")
    void shouldCreateFareRule() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRuleDtos.FareRuleCreateRequest request = givenCreateRequest();
        FareRule savedFareRule = givenFareRule(route, fromStop, toStop);

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));
        when(fareRuleRepository.save(any(FareRule.class))).thenReturn(savedFareRule);

        // When
        FareRuleDtos.FareRuleResponse response = fareRuleService.createFareRule(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.basePrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));

        verify(routeRepository, times(1)).findById(1L);
        verify(stopRepository, times(4)).findById(anyLong()); //dos en el create service y dos en validateStopsBelongToRoute
        verify(fareRuleRepository, times(1)).save(any(FareRule.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando route no existe")
    void shouldThrowExceptionWhenRouteNotFound() {
        // Given
        FareRuleDtos.FareRuleCreateRequest request = givenCreateRequest();

        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fareRuleService.createFareRule(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Route with ID 1 not found");

        verify(routeRepository, times(1)).findById(1L);
        verify(fareRuleRepository, never()).save(any(FareRule.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando fromStop no pertenece a la ruta")
    void shouldThrowExceptionWhenFromStopNotInRoute() {
        // Given
        Route route = givenRoute();
        Route anotherRoute = Route.builder().id(2L).build();
        Stop fromStop = givenStop(1L, "Stop 1", 1, anotherRoute);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRuleDtos.FareRuleCreateRequest request = givenCreateRequest();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));

        // When & Then
        assertThatThrownBy(() -> fareRuleService.createFareRule(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From stop 1 does not belong to route 1");

        verify(fareRuleRepository, never()).save(any(FareRule.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando orden de stops es inválido")
    void shouldThrowExceptionWhenStopOrderInvalid() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 2, route);
        Stop toStop = givenStop(2L, "Stop 2", 1, route);
        FareRuleDtos.FareRuleCreateRequest request = givenCreateRequest();

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(stopRepository.findById(1L)).thenReturn(Optional.of(fromStop));
        when(stopRepository.findById(2L)).thenReturn(Optional.of(toStop));

        // When & Then
        assertThatThrownBy(() -> fareRuleService.createFareRule(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From stop order (2) must be less than to stop order (1)");

        verify(fareRuleRepository, never()).save(any(FareRule.class));
    }

    @Test
    @DisplayName("Debe actualizar fare rule correctamente")
    void shouldUpdateFareRule() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule existingFareRule = givenFareRule(route, fromStop, toStop);
        FareRuleDtos.FareRuleUpdateRequest request = givenUpdateRequest();

        when(fareRuleRepository.findById(1L)).thenReturn(Optional.of(existingFareRule));
        when(fareRuleRepository.save(any(FareRule.class))).thenReturn(existingFareRule);

        // When
        fareRuleService.updateFareRule(1L, request);

        // Then
        verify(fareRuleRepository, times(1)).findById(1L);
        verify(fareRuleRepository, times(1)).save(existingFareRule);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar fare rule inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentFareRule() {
        // Given
        FareRuleDtos.FareRuleUpdateRequest request = givenUpdateRequest();

        when(fareRuleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fareRuleService.updateFareRule(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Fare rule with ID 999 not found");

        verify(fareRuleRepository, times(1)).findById(999L);
        verify(fareRuleRepository, never()).save(any(FareRule.class));
    }

    @Test
    @DisplayName("Debe eliminar fare rule correctamente")
    void shouldDeleteFareRule() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule fareRule = givenFareRule(route, fromStop, toStop);

        when(fareRuleRepository.findById(1L)).thenReturn(Optional.of(fareRule));
        doNothing().when(fareRuleRepository).delete(fareRule);

        // When
        fareRuleService.deleteFareRule(1L);

        // Then
        verify(fareRuleRepository, times(1)).findById(1L);
        verify(fareRuleRepository, times(1)).delete(fareRule);
    }

    @Test
    @DisplayName("Debe obtener fare rule por ID")
    void shouldGetFareRuleById() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule fareRule = givenFareRule(route, fromStop, toStop);

        when(fareRuleRepository.findById(1L)).thenReturn(Optional.of(fareRule));

        // When
        FareRuleDtos.FareRuleResponse response = fareRuleService.getFareRule(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(fareRuleRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe obtener fare rules por route ID")
    void shouldGetFareRulesByRouteId() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        List<FareRule> fareRules = List.of(givenFareRule(route, fromStop, toStop));

        when(routeRepository.existsById(1L)).thenReturn(true);
        when(fareRuleRepository.findByRouteId(1L)).thenReturn(fareRules);

        // When
        List<FareRuleDtos.FareRuleResponse> result = fareRuleService.getFareRulesByRouteId(1L);

        // Then
        assertThat(result).hasSize(1);

        verify(routeRepository, times(1)).existsById(1L);
        verify(fareRuleRepository, times(1)).findByRouteId(1L);
    }

    @Test
    @DisplayName("Debe calcular precio correctamente sin descuentos ni recargos")
    void shouldCalculatePriceWithoutDiscountsOrSurcharges() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule fareRule = givenFareRule(route, fromStop, toStop);
        fareRule.setDiscounts(new HashMap<>()); // Sin descuentos
        fareRule.setDynamicPricing(FareRule.DynamicPricing.OFF);

        Passenger passenger = givenPassenger(30); // Adulto sin descuento
        Seat seat = givenSeat(Seat.Type.STANDARD);

        when(fareRuleRepository.findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L))
                .thenReturn(Optional.of(fareRule));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(seatRepository.findByBusIdAndNumber(1L, "A1")).thenReturn(Optional.of(seat));

        // When
        BigDecimal price = fareRuleService.calculatePrice(1L, 1L, 2L, 1L, 1L, "A1", 1L);

        // Then
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(50000));

        verify(fareRuleRepository, times(1)).findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L);
    }

    @Test
    @DisplayName("Debe aplicar descuento de niño (50%)")
    void shouldApplyChildDiscount() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule fareRule = givenFareRule(route, fromStop, toStop);
        fareRule.setDynamicPricing(FareRule.DynamicPricing.OFF);

        Passenger child = givenPassenger(8); // Niño
        Seat seat = givenSeat(Seat.Type.STANDARD);

        when(fareRuleRepository.findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L))
                .thenReturn(Optional.of(fareRule));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(child));
        when(seatRepository.findByBusIdAndNumber(1L, "A1")).thenReturn(Optional.of(seat));

        // When
        BigDecimal price = fareRuleService.calculatePrice(1L, 1L, 2L, 1L, 1L, "A1", 1L);

        // Then
        // 50000 * (1 - 0.5) = 25000
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(25000));
    }

    @Test
    @DisplayName("Debe calcular precio complejo con descuento + recargo asiento + recargo dinámico")
    void shouldCalculateComplexPrice() {
        // Given
        Route route = givenRoute();
        Stop fromStop = givenStop(1L, "Stop 1", 1, route);
        Stop toStop = givenStop(2L, "Stop 2", 2, route);
        FareRule fareRule = givenFareRule(route, fromStop, toStop);

        Passenger student = givenPassenger(20); // Estudiante 20%
        Seat seat = givenSeat(Seat.Type.PREFERENTIAL); // +15%
        Bus bus = givenBus(40);

        when(fareRuleRepository.findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L))
                .thenReturn(Optional.of(fareRule));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(student));
        when(seatRepository.findByBusIdAndNumber(1L, "A1")).thenReturn(Optional.of(seat));
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(ticketRepository.countSoldByTrip(1L)).thenReturn(35L); // +20% dinámico

        // When
        BigDecimal price = fareRuleService.calculatePrice(1L, 1L, 2L, 1L, 1L, "A1", 1L);

        // Then
        // Base: 50000
        // Descuento estudiante: 50000 * (1 - 0.2) = 40000
        // Recargo asiento: 50000 * 0.15 = 7500
        // Recargo dinámico: 50000 * 0.20 = 10000
        // Total: 40000 + 7500 + 10000 = 57500
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(57500));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando fare rule no existe")
    void shouldThrowExceptionWhenFareRuleNotFoundForCalculation() {
        // Given
        when(fareRuleRepository.findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fareRuleService.calculatePrice(1L, 1L, 2L, 1L, 1L, "A1", 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Fare rule not found for route 1 from stop 1 to stop 2");

        verify(fareRuleRepository, times(1)).findByRouteIdAndFromStopIdAndToStopId(1L, 1L, 2L);
    }
}