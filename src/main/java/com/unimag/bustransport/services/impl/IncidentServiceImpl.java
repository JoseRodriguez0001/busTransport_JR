package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.domain.entities.Incident;
import com.unimag.bustransport.domain.repositories.IncidentRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.IncidentService;
import com.unimag.bustransport.services.mapper.IncidentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;
    @Override
    public IncidentDtos.IncidentResponse createIncident(IncidentDtos.IncidentCreateRequest request) {
        Incident incident = incidentMapper.toEntity(request);
        incident.setCreatedAt(OffsetDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);
        log.info("Incident created successfully");
        return incidentMapper.toResponse(savedIncident);
    }

    @Override
    public void updateIncident(Long incidentId, IncidentDtos.IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new NotFoundException("incident not found"));
        incidentMapper.updateEntityFromRequest(request, incident);
        incidentRepository.save(incident);
        log.info("Incident updated successfully");
    }

    @Override
    public void deleteIncident(Long incidentId) {
        incidentRepository.deleteById(incidentId);
        log.info("Incident deleted successfully");
    }

    @Override
    public List<IncidentDtos.IncidentResponse> findByIncidentByType(Incident.EntityType type) {
        List<Incident> incidents = incidentRepository.findByEntityType(type);
        return incidents.stream()
                .map(incidentMapper::toResponse).toList();
    }

    @Override
    public List<IncidentDtos.IncidentResponse> findIncidentsRecentByType(Incident.Type type) {
        List<Incident> incidents = incidentRepository.findRecentByType(type);
        return incidents.stream()
                .map(incidentMapper::toResponse).toList();
    }
}
