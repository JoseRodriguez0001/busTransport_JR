package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.StopDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.services.StopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StopController.class)
@Import(TestSecurityConfig.class)
class StopControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean StopService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new StopCreateRequest("Terminal Santa Marta", 1, 11.2408, -74.1990, 1L);
        var resp = stopResponse(1L, "Terminal Santa Marta", 1, 11.2408, -74.1990);

        when(service.createStop(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/stops/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Terminal Santa Marta"))
                .andExpect(jsonPath("$.order").value(1));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getStopById(5L))
                .thenReturn(stopResponse(5L, "Terminal Barranquilla", 3, 10.9639, -74.7964));

        mvc.perform(get("/api/v1/stops/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Terminal Barranquilla"));
    }

    @Test
    void getByRoute_shouldReturn200() throws Exception {
        var stops = List.of(
                stopResponse(1L, "Terminal Santa Marta", 1, 11.2408, -74.1990),
                stopResponse(2L, "Parada Ciénaga", 2, 10.9981, -74.2470),
                stopResponse(3L, "Terminal Barranquilla", 3, 10.9639, -74.7964)
        );

        when(service.getStopsByRouteId(1L)).thenReturn(stops);

        mvc.perform(get("/api/v1/stops/by-route/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getStopsBetween_shouldReturn200() throws Exception {
        var stops = List.of(
                stopResponse(2L, "Parada Ciénaga", 2, 10.9981, -74.2470),
                stopResponse(3L, "Terminal Barranquilla", 3, 10.9639, -74.7964)
        );

        when(service.getStopsBetween(1L, 2, 3)).thenReturn(stops);

        mvc.perform(get("/api/v1/stops/by-route/1/between")
                        .param("fromOrder", "2")
                        .param("toOrder", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new StopUpdateRequest("Terminal Santa Marta Actualizado", 1, 11.2408, -74.1990);

        mvc.perform(patch("/api/v1/stops/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateStop(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/stops/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteStop(1L);
    }

    // Helper
    private StopResponse stopResponse(Long id, String name, Integer order, Double lat, Double lng) {
        return new StopResponse(id, name, order, lat, lng);
    }
}