package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.PurchaseDtos.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean PurchaseService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var ticketReq = new PurchaseCreateRequest.TicketRequest(
                1L, 10L, "A12", 1L, 3L,
                new PurchaseCreateRequest.BaggageRequest(15.5, null)
        );
        var req = new PurchaseCreateRequest(5L, "CARD", List.of(ticketReq));
        var resp = purchaseResponse(1L, new BigDecimal("45000.00"), "CARD", "PENDING");

        when(service.createPurchase(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/purchases/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalAmount").value(45000.00))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getPurchase(3L))
                .thenReturn(purchaseResponse(3L, new BigDecimal("85000.00"), "TRANSFER", "CONFIRMED"));

        mvc.perform(get("/api/v1/purchases/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getPurchase(999L)).thenThrow(new NotFoundException("Purchase with ID 999 not found"));

        mvc.perform(get("/api/v1/purchases/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Purchase with ID 999 not found"));
    }

    @Test
    void getByUser_shouldReturn200() throws Exception {
        var purchases = List.of(
                purchaseResponse(1L, new BigDecimal("45000.00"), "CARD", "CONFIRMED"),
                purchaseResponse(2L, new BigDecimal("90000.00"), "CASH", "CONFIRMED")
        );

        when(service.getPurchasesByUserId(5L)).thenReturn(purchases);

        mvc.perform(get("/api/v1/purchases/by-user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByDateRange_shouldReturn200() throws Exception {
        var purchases = List.of(
                purchaseResponse(1L, new BigDecimal("45000.00"), "CARD", "CONFIRMED")
        );

        when(service.getPurchasesByDateRange(any(), any())).thenReturn(purchases);

        mvc.perform(get("/api/v1/purchases/by-date-range")
                        .param("start", "2025-11-01T00:00:00-05:00")
                        .param("end", "2025-11-30T23:59:59-05:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void confirm_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/purchases/1/confirm")
                        .param("paymentReference", "TRX-123456789"))
                .andExpect(status().isNoContent());

        verify(service).confirmPurchase(1L, "TRX-123456789");
    }

    @Test
    void cancel_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/purchases/1/cancel"))
                .andExpect(status().isNoContent());

        verify(service).cancelPurchase(1L);
    }

    // Helper
    private PurchaseResponse purchaseResponse(Long id, BigDecimal amount, String method, String status) {
        var userSummary = new PurchaseResponse.UserSummary(5L, "Robert Martínez", "robert@gmail.com");
        var ticketSummary = new PurchaseResponse.TicketSummary(1L, "A12", new BigDecimal("45000.00"), "SOLD");
        return new PurchaseResponse(id, amount, method, status, OffsetDateTime.now(),
                userSummary, List.of(ticketSummary));
    }
}
