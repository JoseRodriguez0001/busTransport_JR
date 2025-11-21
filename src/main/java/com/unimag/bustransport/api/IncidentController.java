package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.IncidentDtos.IncidentCreateRequest;
import com.unimag.bustransport.api.dto.IncidentDtos.IncidentResponse;
import com.unimag.bustransport.api.dto.IncidentDtos.IncidentUpdateRequest;
import com.unimag.bustransport.domain.entities.Incident;
import com.unimag.bustransport.services.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Validated
public class IncidentController {

    private final IncidentService service;

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentCreateRequest req,
                                                   UriComponentsBuilder uriBuilder) {
        var incidentCreated = service.createIncident(req);
        var location = uriBuilder.path("/api/v1/incidents/{id}")
                .buildAndExpand(incidentCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(incidentCreated);
    }

    @GetMapping("/by-entity-type/{entityType}")
    public ResponseEntity<List<IncidentResponse>> getByEntityType(@PathVariable Incident.EntityType entityType) {
        return ResponseEntity.ok(service.findByIncidentByType(entityType));
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<IncidentResponse>> getByType(@PathVariable Incident.Type type) {
        return ResponseEntity.ok(service.findIncidentsRecentByType(type));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody IncidentUpdateRequest req) {
        service.updateIncident(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}