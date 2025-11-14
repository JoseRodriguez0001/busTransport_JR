package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.ConfigDtos.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(ConfigController.class)
class ConfigControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean ConfigService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new ConfigCreateRequest("max_baggage_weight", "25");
        var resp = new ConfigResponse(1L, "max_baggage_weight", "25");

        when(service.createConfig(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/configs/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.key").value("max_baggage_weight"))
                .andExpect(jsonPath("$.value").value("25"));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        var configs = List.of(
                new ConfigResponse(1L, "max_baggage_weight", "25"),
                new ConfigResponse(2L, "seat_hold_minutes", "15")
        );

        when(service.getAllConfigs()).thenReturn(configs);

        mvc.perform(get("/api/v1/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByKey_shouldReturn200() throws Exception {
        when(service.getConfigByKey("max_baggage_weight"))
                .thenReturn(new ConfigResponse(1L, "max_baggage_weight", "25"));

        mvc.perform(get("/api/v1/configs/by-key/max_baggage_weight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("max_baggage_weight"))
                .andExpect(jsonPath("$.value").value("25"));
    }

    @Test
    void getByKey_shouldReturn404WhenNotFound() throws Exception {
        when(service.getConfigByKey("nonexistent_key"))
                .thenThrow(new NotFoundException("Config with key nonexistent_key not found"));

        mvc.perform(get("/api/v1/configs/by-key/nonexistent_key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Config with key nonexistent_key not found"));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new ConfigUpdateRequest("30");

        mvc.perform(patch("/api/v1/configs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateConfig(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/configs/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteConfig(1L);
    }
}