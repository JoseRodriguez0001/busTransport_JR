package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.BaggageDtos.*;
import com.unimag.bustransport.services.BaggageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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

@WebMvcTest(BaggageController.class)
class BaggageControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean BaggageService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new BaggageCreateRequest(1L, 15.5);
        var resp = baggageResponse(1L, 15.5, new BigDecimal("10000.00"), "BAG-001", 1L);

        when(service.registerBaggage(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/baggage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/baggage/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weightKg").value(15.5))
                .andExpect(jsonPath("$.fee").value(10000.00))
                .andExpect(jsonPath("$.tagCode").value("BAG-001"));
    }

    @Test
    void getByTicket_shouldReturn200() throws Exception {
        var baggages = List.of(
                baggageResponse(1L, 15.5, new BigDecimal("10000.00"), "BAG-001", 1L),
                baggageResponse(2L, 5.0, new BigDecimal("3000.00"), "BAG-002", 1L)
        );

        when(service.getBaggageByTicket(1L)).thenReturn(baggages);

        mvc.perform(get("/api/v1/baggage/by-ticket/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weightKg").value(15.5))
                .andExpect(jsonPath("$[1].weightKg").value(5.0));
    }

    @Test
    void calculateFee_shouldReturn200() throws Exception {
        when(service.calculateBaggageFee(20.0)).thenReturn(new BigDecimal("15000.00"));

        mvc.perform(get("/api/v1/baggage/calculate-fee")
                        .param("weightKg", "20.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(15000.00));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new BaggageUpdateRequest(18.0, "BAG-001-UPD");

        mvc.perform(patch("/api/v1/baggage/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateBaggage(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/baggage/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteBaggage(1L);
    }

    // Helper
    private BaggageResponse baggageResponse(Long id, Double weightKg, BigDecimal fee, String tagCode, Long ticketId) {
        var ticketSummary = new BaggageResponse.TicketSummary(ticketId, "A12", "QR-" + ticketId);
        return new BaggageResponse(id, weightKg, fee, tagCode, ticketSummary);
    }
}
