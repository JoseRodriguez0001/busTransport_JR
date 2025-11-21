package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.SeatDtos.SeatResponse;
import com.unimag.bustransport.api.dto.TripDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import(TestSecurityConfig.class)
class TripControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean TripService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var departure = OffsetDateTime.parse("2025-11-20T08:00:00-05:00");
        var arrival = OffsetDateTime.parse("2025-11-20T10:00:00-05:00");
        var req = new TripCreateRequest(1L, 5L, LocalDate.of(2025, 11, 20), departure, arrival, 10.0);
        var resp = tripResponse(1L, departure, arrival, "Santa Marta", "Barranquilla");

        when(service.createTrip(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/trips/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        var departure = OffsetDateTime.parse("2025-11-20T08:00:00-05:00");
        var arrival = OffsetDateTime.parse("2025-11-20T10:00:00-05:00");

        when(service.getTripDetails(3L))
                .thenReturn(tripResponse(3L, departure, arrival, "Bogotá", "Medellín"));

        mvc.perform(get("/api/v1/trips/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.route.origin").value("Bogotá"));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getTripDetails(999L)).thenThrow(new NotFoundException("Trip with ID 999 not found"));

        mvc.perform(get("/api/v1/trips/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trip with ID 999 not found"));
    }

    @Test
    void search_shouldReturn200() throws Exception {
        var departure = OffsetDateTime.parse("2025-11-20T08:00:00-05:00");
        var arrival = OffsetDateTime.parse("2025-11-20T10:00:00-05:00");
        var trips = List.of(
                tripResponse(1L, departure, arrival, "Santa Marta", "Barranquilla"),
                tripResponse(2L, departure.plusHours(4), arrival.plusHours(4), "Santa Marta", "Barranquilla")
        );

        when(service.getTrips("Santa Marta", "Barranquilla", LocalDate.of(2025, 11, 20)))
                .thenReturn(trips);

        mvc.perform(get("/api/v1/trips/search")
                        .param("origin", "Santa Marta")
                        .param("destination", "Barranquilla")
                        .param("date", "2025-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSeats_shouldReturn200() throws Exception {
        var seats = List.of(
                new SeatResponse(1L, "A1", "STANDARD", 5L, "ABC-123", false),
                new SeatResponse(2L, "A2", "STANDARD", 5L, "ABC-123", true)
        );

        when(service.getSeats(1L)).thenReturn(seats);

        mvc.perform(get("/api/v1/trips/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getStatistics_shouldReturn200() throws Exception {
        when(service.getTripStatistics(1L)).thenReturn(25L);

        mvc.perform(get("/api/v1/trips/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldSeats").value(25));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new TripUpdateRequest(
                OffsetDateTime.parse("2025-11-20T09:00:00-05:00"),
                OffsetDateTime.parse("2025-11-20T11:00:00-05:00"),
                15.0,
                Trip.Status.BOARDING
        );

        mvc.perform(patch("/api/v1/trips/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateTrip(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/trips/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteTrip(1L);
    }

    // Helper
    private TripResponse tripResponse(Long id, OffsetDateTime departure, OffsetDateTime arrival,
                                      String origin, String destination) {
        var routeSummary = new TripResponse.RouteSummary(1L, "SM-BAQ-01", origin, destination);
        var busSummary = new TripResponse.BusSummary(5L, "ABC-123", 40);
        return new TripResponse(id, routeSummary, busSummary, LocalDate.now(), departure, arrival,
                120, 10.0, "SCHEDULED", 15, 25);
    }
}
