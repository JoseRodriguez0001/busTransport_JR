package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.KpiDtos;
import com.unimag.bustransport.domain.entities.Kpi;
import com.unimag.bustransport.domain.repositories.KpiRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.KpiMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KpiService Unit Tests")
class KpiServiceImplTest {

    @Mock
    private KpiRepository kpiRepository;

    private final KpiMapper kpiMapper = Mappers.getMapper(KpiMapper.class);

    private KpiServiceImpl kpiService;

    @BeforeEach
    void setUp() {
        kpiService = new KpiServiceImpl(
                kpiRepository,
                kpiMapper
        );
    }

    private Kpi givenKpi(String name, Double value) {
        return Kpi.builder()
                .id(1L)
                .name(name)
                .value(value)
                .calculatedAt(OffsetDateTime.now())
                .build();
    }

    private KpiDtos.KpiCreateRequest givenCreateRequest() {
        return new KpiDtos.KpiCreateRequest(
                "revenue_total",
                150000.50
        );
    }

    private KpiDtos.KpiUpdateRequest givenUpdateRequest() {
        return new KpiDtos.KpiUpdateRequest(
                200000.75
        );
    }

    @Test
    @DisplayName("Debe crear KPI correctamente")
    void shouldCreateKpi() {
        // Given
        KpiDtos.KpiCreateRequest request = givenCreateRequest();
        Kpi savedKpi = givenKpi("revenue_total", 150000.50);

        when(kpiRepository.save(any(Kpi.class))).thenReturn(savedKpi);

        // When
        KpiDtos.KpiResponse response = kpiService.createKpi(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("revenue_total");
        assertThat(response.value()).isEqualTo(150000.50);

        verify(kpiRepository, times(1)).save(any(Kpi.class));
    }

    @Test
    @DisplayName("Debe actualizar KPI correctamente")
    void shouldUpdateKpi() {
        // Given
        Kpi existingKpi = givenKpi("revenue_total", 150000.50);
        KpiDtos.KpiUpdateRequest request = givenUpdateRequest();

        when(kpiRepository.findById(1L)).thenReturn(Optional.of(existingKpi));
        when(kpiRepository.save(any(Kpi.class))).thenReturn(existingKpi);

        // When
        kpiService.updateKpi(1L, request);

        // Then
        verify(kpiRepository, times(1)).findById(1L);
        verify(kpiRepository, times(1)).save(existingKpi);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar KPI inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentKpi() {
        // Given
        KpiDtos.KpiUpdateRequest request = givenUpdateRequest();

        when(kpiRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kpiService.updateKpi(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Kpi not found");

        verify(kpiRepository, times(1)).findById(999L);
        verify(kpiRepository, never()).save(any(Kpi.class));
    }

    @Test
    @DisplayName("Debe eliminar KPI correctamente")
    void shouldDeleteKpi() {
        // Given
        Kpi kpi = givenKpi("old_metric", 100.0);

        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpi));
        doNothing().when(kpiRepository).delete(kpi);

        // When
        kpiService.deleteKpi(1L);

        // Then
        verify(kpiRepository, times(1)).findById(1L);
        verify(kpiRepository, times(1)).delete(kpi);
    }

    @Test
    @DisplayName("Debe obtener KPI por nombre")
    void shouldGetKpiByName() {
        // Given
        Kpi kpi = givenKpi("occupancy_rate", 85.5);

        when(kpiRepository.findByName("occupancy_rate")).thenReturn(Optional.of(kpi));

        // When
        KpiDtos.KpiResponse response = kpiService.getKpiByName("occupancy_rate");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("occupancy_rate");
        assertThat(response.value()).isEqualTo(85.5);

        verify(kpiRepository, times(1)).findByName("occupancy_rate");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando KPI no existe por nombre")
    void shouldThrowExceptionWhenKpiNotFoundByName() {
        // Given
        when(kpiRepository.findByName("non_existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kpiService.getKpiByName("non_existent"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Kpi not found");

        verify(kpiRepository, times(1)).findByName("non_existent");
    }

    @Test
    @DisplayName("Debe obtener métricas recientes ordenadas")
    void shouldGetRecentMetrics() {
        // Given
        Kpi kpi1 = givenKpi("metric1", 100.0);
        kpi1.setCalculatedAt(OffsetDateTime.now().minusHours(1));

        Kpi kpi2 = givenKpi("metric2", 200.0);
        kpi2.setCalculatedAt(OffsetDateTime.now());

        List<Kpi> kpis = List.of(kpi2, kpi1); // Ordenados DESC

        when(kpiRepository.findRecentMetrics()).thenReturn(kpis);

        // When
        List<KpiDtos.KpiResponse> result = kpiService.getKpiByRecentMetrics();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("metric2");
        assertThat(result.get(1).name()).isEqualTo("metric1");

        verify(kpiRepository, times(1)).findRecentMetrics();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay métricas recientes")
    void shouldReturnEmptyListWhenNoRecentMetrics() {
        // Given
        when(kpiRepository.findRecentMetrics()).thenReturn(List.of());

        // When
        List<KpiDtos.KpiResponse> result = kpiService.getKpiByRecentMetrics();

        // Then
        assertThat(result).isEmpty();

        verify(kpiRepository, times(1)).findRecentMetrics();
    }
}