package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.SeatHoldDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.SeatHoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatHoldController.class)
@Import(TestSecurityConfig.class)
class SeatHoldControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean SeatHoldService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new SeatHoldCreateRequest("A12", 1L, 5L);
        var resp = holdResponse(1L, "A12", "HOLD");

        when(service.createSeatHold(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/seat-holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/seat-holds/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.seatNumber").value("A12"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getHoldById(3L))
                .thenReturn(holdResponse(3L, "B5", "HOLD"));

        mvc.perform(get("/api/v1/seat-holds/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.seatNumber").value("B5"));
    }

    @Test
    void getByUser_shouldReturn200() throws Exception {
        var holds = List.of(
                holdResponse(1L, "A12", "HOLD"),
                holdResponse(2L, "A13", "HOLD")
        );

        when(service.getHoldsByUser(5L)).thenReturn(holds);

        mvc.perform(get("/api/v1/seat-holds/by-user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByTrip_shouldReturn200() throws Exception {
        var holds = List.of(
                holdResponse(1L, "A12", "HOLD")
        );

        when(service.getActiveHoldsByTrip(1L)).thenReturn(holds);

        mvc.perform(get("/api/v1/seat-holds/by-trip/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void checkHold_shouldReturn200() throws Exception {
        when(service.isSeatOnHold(1L, "A12")).thenReturn(true);

        mvc.perform(get("/api/v1/seat-holds/check-hold")
                        .param("tripId", "1")
                        .param("seatNumber", "A12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOnHold").value(true));
    }

    @Test
    void release_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/seat-holds/1/release"))
                .andExpect(status().isNoContent());

        verify(service).releaseSeatHold(1L);
    }

    @Test
    void markExpired_shouldReturn200() throws Exception {
        when(service.markExpiredHolds()).thenReturn(5);

        mvc.perform(post("/api/v1/seat-holds/cleanup/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.action").value("Marked as expired"));
    }

    @Test
    void deleteExpired_shouldReturn200() throws Exception {
        when(service.deleteExpiredHolds()).thenReturn(3);

        mvc.perform(delete("/api/v1/seat-holds/cleanup/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.action").value("Deleted"));
    }

    // Helper
    private SeatHoldResponse holdResponse(Long id, String seatNumber, String status) {
        var tripSummary = new SeatHoldResponse.TripSummary(1L, "Santa Marta", "Barranquilla",
                OffsetDateTime.parse("2025-11-20T08:00:00-05:00"));
        var userSummary = new SeatHoldResponse.UserSummary(5L, "Robert Martínez");
        return new SeatHoldResponse(id, seatNumber, status, OffsetDateTime.now().plusMinutes(15),
                tripSummary, userSummary);
    }
}
