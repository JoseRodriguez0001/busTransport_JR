package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.SeatDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.SeatService;
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

@WebMvcTest(SeatController.class)
@Import(TestSecurityConfig.class)
class SeatControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean SeatService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new SeatCreateRequest("A1", Seat.Type.STANDARD , 1L);
        var resp = new SeatResponse(1L, "A1", "STANDARD", 1L, "ABC-123", false);

        when(service.createSeat(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/seats/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value("A1"))
                .andExpect(jsonPath("$.type").value("STANDARD"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getSeatById(5L))
                .thenReturn(new SeatResponse(5L, "B12", "PREFERENTIAL", 2L, "XYZ-789", false));

        mvc.perform(get("/api/v1/seats/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.number").value("B12"));
    }

    @Test
    void getByBus_shouldReturn200() throws Exception {
        var seats = List.of(
                new SeatResponse(1L, "A1", "STANDARD", 1L, "ABC-123", false),
                new SeatResponse(2L, "A2", "STANDARD", 1L, "ABC-123", false)
        );

        when(service.getSeatsByBusId(1L)).thenReturn(seats);

        mvc.perform(get("/api/v1/seats/by-bus/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByBusAndType_shouldReturn200() throws Exception {
        var seats = List.of(
                new SeatResponse(1L, "P1", "PREFERENTIAL", 1L, "ABC-123", false)
        );

        when(service.getSeatsByBusIdAndType(1L, Seat.Type.PREFERENTIAL)).thenReturn(seats);

        mvc.perform(get("/api/v1/seats/by-bus/1/type/PREFERENTIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void checkAvailability_shouldReturn200() throws Exception {
        when(service.isSeatAvailable(10L, "A1", 1L, 5L)).thenReturn(true);

        mvc.perform(get("/api/v1/seats/check-availability")
                        .param("tripId", "10")
                        .param("seatNumber", "A1")
                        .param("fromStopId", "1")
                        .param("toStopId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void confirmReservation_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/seats/confirm-reservation")
                        .param("tripId", "10")
                        .param("seatNumbers", "A1", "A2")
                        .param("purchaseId", "100"))
                .andExpect(status().isNoContent());

        verify(service).confirmSeatReservation(eq(10L), eq(List.of("A1", "A2")), eq(100L));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new SeatUpdateRequest("A1", Seat.Type.PREFERENTIAL);

        mvc.perform(patch("/api/v1/seats/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateSeat(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/seats/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteSeat(1L);
    }
}
