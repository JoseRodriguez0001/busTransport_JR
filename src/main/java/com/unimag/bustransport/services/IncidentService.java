package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.domain.entities.Incident;

import java.util.List;

public interface IncidentService {
    IncidentDtos.IncidentResponse createIncident(IncidentDtos.IncidentCreateRequest request);
    void updateIncident(Long incidentId,IncidentDtos.IncidentUpdateRequest request);
    void deleteIncident(Long incidentId);
    List<IncidentDtos.IncidentResponse> findByIncidentByType(Incident.EntityType type); //por tipo de entidad
    List<IncidentDtos.IncidentResponse> findIncidentsRecentByType(Incident.Type type);
}
