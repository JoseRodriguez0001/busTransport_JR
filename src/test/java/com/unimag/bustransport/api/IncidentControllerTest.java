package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.IncidentDtos.*;
import com.unimag.bustransport.domain.entities.Incident;
import com.unimag.bustransport.services.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean IncidentService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new IncidentCreateRequest("TRIP", 10L, "SECURITY", "Pasajero sospechoso reportado en el viaje");
        var resp = incidentResponse(1L, "TRIP", 10L, "SECURITY", "Pasajero sospechoso reportado en el viaje");

        when(service.createIncident(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/incidents/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.entityType").value("TRIP"))
                .andExpect(jsonPath("$.type").value("SECURITY"))
                .andExpect(jsonPath("$.entityId").value(10));
    }

    @Test
    void getByEntityType_shouldReturn200() throws Exception {
        var incidents = List.of(
                incidentResponse(1L, "TRIP", 10L, "SECURITY", "Pasajero sospechoso reportado en el viaje"),
                incidentResponse(2L, "TRIP", 11L, "DELAY", "Retraso de 30 minutos por tráfico")
        );

        when(service.findByIncidentByType(Incident.EntityType.TRIP)).thenReturn(incidents);

        mvc.perform(get("/api/v1/incidents/by-entity-type/TRIP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByType_shouldReturn200() throws Exception {
        var incidents = List.of(
                incidentResponse(1L, "TRIP", 10L, "SECURITY", "Pasajero sospechoso reportado en el viaje")
        );

        when(service.findIncidentsRecentByType(Incident.Type.SECURITY)).thenReturn(incidents);

        mvc.perform(get("/api/v1/incidents/by-type/SECURITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new IncidentUpdateRequest("MECHANICAL", "Falla mecánica solucionada");

        mvc.perform(patch("/api/v1/incidents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateIncident(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/incidents/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteIncident(1L);
    }

    // Helper
    private IncidentResponse incidentResponse(Long id, String entityType, Long entityId, String type, String note) {
        return new IncidentResponse(id, entityType, entityId, type, note,
                OffsetDateTime.parse("2025-11-20T10:30:00-05:00"));
    }
}
