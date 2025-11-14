package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.KpiDtos.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.KpiService;
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

@WebMvcTest(KpiController.class)
class KpiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean KpiService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new KpiCreateRequest("occupancy_rate", 85.5);
        var resp = kpiResponse(1L, "occupancy_rate", 85.5);

        when(service.createKpi(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/kpis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/kpis/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("occupancy_rate"))
                .andExpect(jsonPath("$.value").value(85.5));
    }

    @Test
    void getByName_shouldReturn200() throws Exception {
        when(service.getKpiByName("occupancy_rate"))
                .thenReturn(kpiResponse(1L, "occupancy_rate", 85.5));

        mvc.perform(get("/api/v1/kpis/by-name/occupancy_rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("occupancy_rate"))
                .andExpect(jsonPath("$.value").value(85.5));
    }

    @Test
    void getByName_shouldReturn404WhenNotFound() throws Exception {
        when(service.getKpiByName("nonexistent_kpi"))
                .thenThrow(new NotFoundException("KPI with name nonexistent_kpi not found"));

        mvc.perform(get("/api/v1/kpis/by-name/nonexistent_kpi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("KPI with name nonexistent_kpi not found"));
    }

    @Test
    void getRecent_shouldReturn200() throws Exception {
        var kpis = List.of(
                kpiResponse(1L, "occupancy_rate", 85.5),
                kpiResponse(2L, "on_time_rate", 92.3),
                kpiResponse(3L, "revenue_per_trip", 1250000.00)
        );

        when(service.getKpiByRecentMetrics()).thenReturn(kpis);

        mvc.perform(get("/api/v1/kpis/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new KpiUpdateRequest(88.2);

        mvc.perform(patch("/api/v1/kpis/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateKpi(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/kpis/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteKpi(1L);
    }

    // Helper
    private KpiResponse kpiResponse(Long id, String name, Double value) {
        return new KpiResponse(id, name, value,
                OffsetDateTime.parse("2025-11-20T00:00:00-05:00"));
    }
}