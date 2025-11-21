package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.AssignmentDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.AssignmentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssignmentController.class)
@Import(TestSecurityConfig.class)
class AssignmentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean AssignmentService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new AssignmentCreateRequest(10L, 1L, 2L, true);
        var resp = assignmentResponse(1L, 10L, 1L, 2L, true);

        when(service.createAssignment(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/assignments/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.checklistOk").value(true))
                .andExpect(jsonPath("$.driver.name").value("Carlos Pérez"))
                .andExpect(jsonPath("$.dispatcher.name").value("Jose García"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getAssignment(3L))
                .thenReturn(assignmentResponse(3L, 10L, 1L, 2L, true));

        mvc.perform(get("/api/v1/assignments/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.checklistOk").value(true));
    }

    @Test
    void getByTrip_shouldReturn200() throws Exception {
        when(service.getAssignmentByTripId(10L))
                .thenReturn(assignmentResponse(1L, 10L, 1L, 2L, false));

        mvc.perform(get("/api/v1/assignments/by-trip/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trip.id").value(10))
                .andExpect(jsonPath("$.trip.origin").value("Santa Marta"));
    }

    @Test
    void getByDriver_shouldReturn200() throws Exception {
        var assignments = List.of(
                assignmentResponse(1L, 10L, 1L, 2L, true),
                assignmentResponse(2L, 11L, 1L, 3L, true)
        );

        when(service.getAssignmentByDriverId(1L)).thenReturn(assignments);

        mvc.perform(get("/api/v1/assignments/by-driver/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new AssignmentUpdateRequest(false);

        mvc.perform(patch("/api/v1/assignments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateAssignment(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/assignments/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteAssignment(1L);
    }

    // Helper
    private AssignmentResponse assignmentResponse(Long id, Long tripId, Long driverId, Long dispatcherId, Boolean checklistOk) {
        var tripSummary = new AssignmentResponse.TripSummary(
                tripId,
                "Santa Marta",
                "Barranquilla",
                OffsetDateTime.parse("2025-11-20T08:00:00-05:00")
        );
        var driverSummary = new AssignmentResponse.UserSummary(driverId, "Carlos Pérez");
        var dispatcherSummary = new AssignmentResponse.UserSummary(dispatcherId, "Jose García");

        return new AssignmentResponse(
                id,
                checklistOk,
                OffsetDateTime.parse("2025-11-19T15:00:00-05:00"),
                tripSummary,
                driverSummary,
                dispatcherSummary
        );
    }
}