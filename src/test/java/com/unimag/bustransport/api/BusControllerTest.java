package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.BusDtos.*;
import com.unimag.bustransport.api.dto.SeatDtos.SeatResponse;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.domain.entities.Bus;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.BusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusController.class)
@Import(TestSecurityConfig.class)
class BusControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean BusService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new BusCreateRequest("ABC-123", 40, List.of("WiFi", "Aire acondicionado"), Bus.Status.ACTIVE);
        var resp = new BusResponse(1L, "ABC-123", 40, List.of("WiFi", "Aire acondicionado"), "ACTIVE", 40);

        when(service.createBus(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/buses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/buses/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.plate").value("ABC-123"))
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getBus(5L))
                .thenReturn(new BusResponse(5L, "XYZ-789", 45, List.of("WiFi", "Baño"), "ACTIVE", 45));

        mvc.perform(get("/api/v1/buses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.plate").value("XYZ-789"))
                .andExpect(jsonPath("$.capacity").value(45));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getBus(999L)).thenThrow(new NotFoundException("Bus with ID 999 not found"));

        mvc.perform(get("/api/v1/buses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bus with ID 999 not found"));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        var buses = List.of(
                new BusResponse(1L, "ABC-123", 40, List.of("WiFi"), "ACTIVE", 40),
                new BusResponse(2L, "DEF-456", 42, List.of("WiFi", "Aire acondicionado"), "ACTIVE", 42)
        );

        when(service.getAllBus()).thenReturn(buses);

        mvc.perform(get("/api/v1/buses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllSeats_shouldReturn200() throws Exception {
        var seats = List.of(
                new SeatResponse(1L, "A1", "STANDARD", 1L, "ABC-123", false),
                new SeatResponse(2L, "A2", "PREFERENTIAL", 1L, "ABC-123", false)
        );

        when(service.getAllSeatsByBusId(1L)).thenReturn(seats);

        mvc.perform(get("/api/v1/buses/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new BusUpdateRequest(42, List.of("WiFi", "Aire acondicionado", "Baño"), Bus.Status.IN_REPAIR);

        mvc.perform(patch("/api/v1/buses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateBus(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/buses/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteBus(1L);
    }
}