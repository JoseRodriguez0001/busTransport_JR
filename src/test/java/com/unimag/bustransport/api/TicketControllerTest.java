package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.TicketDtos.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean TicketService service;

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getTicket(1L))
                .thenReturn(ticketResponse(1L, "A12", new BigDecimal("45000.00"), "SOLD"));

        mvc.perform(get("/api/v1/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.seatNumber").value("A12"))
                .andExpect(jsonPath("$.price").value(45000.00));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getTicket(999L)).thenThrow(new NotFoundException("Ticket no encontrado"));

        mvc.perform(get("/api/v1/tickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket no encontrado"));
    }

    @Test
    void getByTrip_shouldReturn200() throws Exception {
        var tickets = List.of(
                ticketResponse(1L, "A12", new BigDecimal("45000.00"), "SOLD"),
                ticketResponse(2L, "A13", new BigDecimal("45000.00"), "SOLD")
        );

        when(service.getTicketsByTrip(1L)).thenReturn(tickets);

        mvc.perform(get("/api/v1/tickets/by-trip/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByPurchase_shouldReturn200() throws Exception {
        var tickets = List.of(
                ticketResponse(1L, "A12", new BigDecimal("45000.00"), "SOLD")
        );

        when(service.getTicketsByPurchase(10L)).thenReturn(tickets);

        mvc.perform(get("/api/v1/tickets/by-purchase/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getByPassenger_shouldReturn200() throws Exception {
        var tickets = List.of(
                ticketResponse(1L, "A12", new BigDecimal("45000.00"), "SOLD"),
                ticketResponse(5L, "B5", new BigDecimal("42000.00"), "SOLD")
        );

        when(service.getTicketsByPassenger(3L)).thenReturn(tickets);

        mvc.perform(get("/api/v1/tickets/by-passenger/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void generateQr_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/tickets/1/generate-qr"))
                .andExpect(status().isNoContent());

        verify(service).generateQrForTicket(1L);
    }

    @Test
    void validateQr_shouldReturn200WithValidTicket() throws Exception {
        mvc.perform(post("/api/v1/tickets/validate-qr")
                        .param("qrCode", "QR-VALID-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Valid ticket"));
    }

    @Test
    void validateQr_shouldReturn200WithInvalidTicket() throws Exception {
        doThrow(new IllegalStateException("Ticket already used"))
                .when(service).validateQrForTicket("QR-INVALID-999");

        mvc.perform(post("/api/v1/tickets/validate-qr")
                        .param("qrCode", "QR-INVALID-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Ticket already used"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/tickets/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteTicket(1L);
    }

    // Helper
    private TicketResponse ticketResponse(Long id, String seatNumber, BigDecimal price, String status) {
        var tripSummary = new TicketResponse.TripSummary(1L, "Santa Marta", "Barranquilla",
                OffsetDateTime.parse("2025-11-20T08:00:00-05:00"));
        var passengerSummary = new TicketResponse.PassengerSummary(3L, "Robert Martínez", "1234567890");
        var stopFrom = new TicketResponse.StopSummary(1L, "Terminal Santa Marta");
        var stopTo = new TicketResponse.StopSummary(3L, "Terminal Barranquilla");
        var purchaseSummary = new TicketResponse.PurchaseSummary(10L, new BigDecimal("45000.00"), "CONFIRMED");
        return new TicketResponse(id, seatNumber, price, status, "QR-123456",
                tripSummary, passengerSummary, stopFrom, stopTo, purchaseSummary);
    }
}
