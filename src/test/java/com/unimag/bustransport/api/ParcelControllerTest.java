package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.ParcelDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.ParcelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParcelController.class)
@Import(TestSecurityConfig.class)
class ParcelControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean ParcelService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;


    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new ParcelCreateRequest(new BigDecimal("25000.00"), "Robert Martínez", "+573001234567",
                "Claudia López", "+573007654321", 1L, 3L, 10L);
        var resp = parcelResponse(1L, "PCL-001", "PENDING");

        when(service.createParcel(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/parcels/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("PCL-001"))
                .andExpect(jsonPath("$.senderName").value("Robert Martínez"));
    }

    @Test
    void getByCode_shouldReturn200() throws Exception {
        when(service.getParcelByCode("PCL-001"))
                .thenReturn(parcelResponse(1L, "PCL-001", "IN_TRANSIT"));

        mvc.perform(get("/api/v1/parcels/by-code/PCL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PCL-001"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void getByCode_shouldReturn404WhenNotFound() throws Exception {
        when(service.getParcelByCode("INVALID"))
                .thenThrow(new NotFoundException("Parcel with code INVALID not found"));

        mvc.perform(get("/api/v1/parcels/by-code/INVALID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Parcel with code INVALID not found"));
    }

    @Test
    void getBySender_shouldReturn200() throws Exception {
        var parcels = List.of(
                parcelResponse(1L, "PCL-001", "IN_TRANSIT"),
                parcelResponse(2L, "PCL-002", "DELIVERED")
        );

        when(service.getParcelsBySender("+573001234567")).thenReturn(parcels);

        mvc.perform(get("/api/v1/parcels/by-sender")
                        .param("phone", "+573001234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByReceiver_shouldReturn200() throws Exception {
        var parcels = List.of(
                parcelResponse(1L, "PCL-001", "IN_TRANSIT")
        );

        when(service.getParcelsByReceiver("+573007654321")).thenReturn(parcels);

        mvc.perform(get("/api/v1/parcels/by-receiver")
                        .param("phone", "+573007654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByTrip_shouldReturn200() throws Exception {
        var parcels = List.of(
                parcelResponse(1L, "PCL-001", "IN_TRANSIT")
        );

        when(service.getParcelsByTrip(10L)).thenReturn(parcels);

        mvc.perform(get("/api/v1/parcels/by-trip/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new ParcelUpdateRequest("Roberto Martínez García", "+573001111111",
                "Claudia López Hernández", "+573007777777", new BigDecimal("30000.00"), 1L, 3L);

        mvc.perform(patch("/api/v1/parcels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateParcel(eq(1L), any());
    }

    @Test
    void assignTrip_shouldReturn204() throws Exception {
        mvc.perform(patch("/api/v1/parcels/1/assign-trip")
                        .param("tripId", "10"))
                .andExpect(status().isNoContent());

        verify(service).assignTrip(1L, 10L);
    }

    @Test
    void confirmDelivery_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/parcels/1/confirm-delivery")
                        .param("otp", "123456")
                        .param("proofPhotoUrl", "https://example.com/photo.jpg"))
                .andExpect(status().isNoContent());

        verify(service).confirmDelivery(1L, "123456", "https://example.com/photo.jpg");
    }

    @Test
    void confirmDelivery_withoutProof_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/parcels/1/confirm-delivery")
                        .param("otp", "123456"))
                .andExpect(status().isNoContent());

        verify(service).confirmDelivery(1L, "123456", null);
    }

    @Test
    void markFailed_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/parcels/1/mark-failed")
                        .param("failureReason", "Destinatario no encontrado"))
                .andExpect(status().isNoContent());

        verify(service).markAsFailed(1L, "Destinatario no encontrado");
    }

    // Helper
    private ParcelResponse parcelResponse(Long id, String code, String status) {
        var tripSummary = new ParcelResponse.TripSummary(10L, "Santa Marta", "Barranquilla");
        var fromStopSummary = new ParcelResponse.StopSummary(1L, "Terminal Santa Marta", "Santa Marta");
        var toStopSummary = new ParcelResponse.StopSummary(3L, "Terminal Barranquilla", "Barranquilla");

        return new ParcelResponse(
                id,
                code,
                new BigDecimal("25000.00"),
                status,
                null,
                "123456",
                "Robert Martínez",
                "+573001234567",
                "Claudia López",
                "+573007654321",
                tripSummary,
                fromStopSummary,
                toStopSummary
        );
    }
}