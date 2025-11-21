package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.RouteDtos.*;
import com.unimag.bustransport.api.dto.StopDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.RouteService;
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

@WebMvcTest(RouteController.class)
@Import(TestSecurityConfig.class)
class RouteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean RouteService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new RouteCreateRequest("SM-BAQ-01", "Ruta Santa Marta - Barranquilla",
                "Santa Marta", "Barranquilla", 95.5, 120);
        var resp = routeResponse(1L, "SM-BAQ-01", "Ruta Santa Marta - Barranquilla",
                "Santa Marta", "Barranquilla", 95.5, 120);

        when(service.createRoute(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/routes/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SM-BAQ-01"))
                .andExpect(jsonPath("$.origin").value("Santa Marta"))
                .andExpect(jsonPath("$.destination").value("Barranquilla"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getRouteById(5L))
                .thenReturn(routeResponse(5L, "BOG-MED-01", "Ruta Bogotá - Medellín",
                        "Bogotá", "Medellín", 415.0, 540));

        mvc.perform(get("/api/v1/routes/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.origin").value("Bogotá"))
                .andExpect(jsonPath("$.destination").value("Medellín"));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getRouteById(999L)).thenThrow(new NotFoundException("Route with ID 999 not found"));

        mvc.perform(get("/api/v1/routes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Route with ID 999 not found"));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        var routes = List.of(
                routeResponse(1L, "SM-BAQ-01", "Ruta SM-BAQ", "Santa Marta", "Barranquilla", 95.5, 120),
                routeResponse(2L, "BAQ-CTG-01", "Ruta BAQ-CTG", "Barranquilla", "Cartagena", 110.0, 150)
        );

        when(service.getAllRoutes()).thenReturn(routes);

        mvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void search_shouldReturn200() throws Exception {
        var routes = List.of(
                routeResponse(1L, "SM-BAQ-01", "Ruta SM-BAQ", "Santa Marta", "Barranquilla", 95.5, 120)
        );

        when(service.searchRoutes("Santa Marta", "Barranquilla")).thenReturn(routes);

        mvc.perform(get("/api/v1/routes/search")
                        .param("origin", "Santa Marta")
                        .param("destination", "Barranquilla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origin").value("Santa Marta"));
    }

    @Test
    void getStops_shouldReturn200() throws Exception {
        var stops = List.of(
                new StopResponse(1L, "Terminal Santa Marta", 1, 11.2408, -74.2011),
                new StopResponse(2L, "Ciénaga", 2, 11.0058, -74.2466),
                new StopResponse(3L, "Terminal Barranquilla", 3, 10.9639, -74.7964)
        );

        when(service.getStopsByRouteId(1L)).thenReturn(stops);

        mvc.perform(get("/api/v1/routes/1/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Terminal Santa Marta"));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new RouteUpdateRequest("Ruta Actualizada", "Santa Marta", "Barranquilla", 96.0, 125);

        mvc.perform(patch("/api/v1/routes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateRoute(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/routes/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteRoute(1L);
    }

    // Helper
    private RouteResponse routeResponse(Long id, String code, String name, String origin,
                                        String destination, Double distanceKm, Integer durationMin) {
        return new RouteResponse(id, code, name, origin, destination, distanceKm, durationMin, List.of(), List.of());
    }
}
