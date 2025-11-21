package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.domain.entities.Incident;
import com.unimag.bustransport.domain.repositories.IncidentRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.IncidentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Spy
    private final IncidentMapper incidentMapper = Mappers.getMapper(IncidentMapper.class);

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private Incident givenIncident() {
        return Incident.builder()
                .id(1L)
                .entityType(Incident.EntityType.TRIP)
                .entityId(100L)
                .type(Incident.Type.SECURITY)
                .note("Test incident")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private IncidentDtos.IncidentCreateRequest givenCreateRequest() {
        return new IncidentDtos.IncidentCreateRequest(
                "TRIP",
                100L,
                "SECURITY",
                "Test incident note"
        );
    }

    private IncidentDtos.IncidentUpdateRequest givenUpdateRequest() {
        return new IncidentDtos.IncidentUpdateRequest(
                "VEHICLE",
                "Updated note"
        );
    }

    @Test
    @DisplayName("Debe crear incident correctamente")
    void shouldCreateIncident() {
        // Given
        IncidentDtos.IncidentCreateRequest request = givenCreateRequest();
        Incident savedIncident = givenIncident();

        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // When
        IncidentDtos.IncidentResponse response = incidentService.createIncident(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.entityType()).isEqualTo("TRIP");
        assertThat(response.type()).isEqualTo("SECURITY");

        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    @DisplayName("Debe actualizar incident correctamente")
    void shouldUpdateIncident() {
        // Given
        Incident existingIncident = givenIncident();
        IncidentDtos.IncidentUpdateRequest request = givenUpdateRequest();

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(existingIncident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(existingIncident);

        // When
        incidentService.updateIncident(1L, request);

        // Then
        verify(incidentRepository, times(1)).findById(1L);
        verify(incidentRepository, times(1)).save(existingIncident);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar incident inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentIncident() {
        // Given
        IncidentDtos.IncidentUpdateRequest request = givenUpdateRequest();

        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> incidentService.updateIncident(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("incident not found");

        verify(incidentRepository, times(1)).findById(999L);
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("Debe eliminar incident correctamente")
    void shouldDeleteIncident() {
        // Given
        doNothing().when(incidentRepository).deleteById(1L);

        // When
        incidentService.deleteIncident(1L);

        // Then
        verify(incidentRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe encontrar incidents por entity type")
    void shouldFindIncidentsByEntityType() {
        // Given
        List<Incident> incidents = List.of(
                givenIncident(),
                givenIncident()
        );

        when(incidentRepository.findByEntityType(Incident.EntityType.TRIP))
                .thenReturn(incidents);

        // When
        List<IncidentDtos.IncidentResponse> result =
                incidentService.findByIncidentByType(Incident.EntityType.TRIP);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(i -> i.entityType().equals("TRIP"));

        verify(incidentRepository, times(1)).findByEntityType(Incident.EntityType.TRIP);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay incidents del entity type")
    void shouldReturnEmptyListWhenNoIncidentsFoundByEntityType() {
        // Given
        when(incidentRepository.findByEntityType(Incident.EntityType.BUS))
                .thenReturn(List.of());

        // When
        List<IncidentDtos.IncidentResponse> result =
                incidentService.findByIncidentByType(Incident.EntityType.BUS);

        // Then
        assertThat(result).isEmpty();

        verify(incidentRepository, times(1)).findByEntityType(Incident.EntityType.BUS);
    }

    @Test
    @DisplayName("Debe encontrar incidents recientes por type")
    void shouldFindRecentIncidentsByType() {
        // Given
        Incident incident1 = givenIncident();
        incident1.setCreatedAt(OffsetDateTime.now().minusHours(1));

        Incident incident2 = givenIncident();
        incident2.setCreatedAt(OffsetDateTime.now().minusHours(2));

        List<Incident> incidents = List.of(incident1, incident2);

        when(incidentRepository.findRecentByType(Incident.Type.SECURITY))
                .thenReturn(incidents);

        // When
        List<IncidentDtos.IncidentResponse> result =
                incidentService.findIncidentsRecentByType(Incident.Type.SECURITY);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(i -> i.type().equals("SECURITY"));

        verify(incidentRepository, times(1)).findRecentByType(Incident.Type.SECURITY);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay incidents recientes del type")
    void shouldReturnEmptyListWhenNoRecentIncidentsFound() {
        // Given
        when(incidentRepository.findRecentByType(Incident.Type.DELIVERY_FAIL))
                .thenReturn(List.of());

        // When
        List<IncidentDtos.IncidentResponse> result =
                incidentService.findIncidentsRecentByType(Incident.Type.DELIVERY_FAIL);

        // Then
        assertThat(result).isEmpty();

        verify(incidentRepository, times(1)).findRecentByType(Incident.Type.DELIVERY_FAIL);
    }
}