package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.FareRuleDtos.*;
import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.services.FareRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FareRuleController.class)
class FareRuleControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean FareRuleService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var discounts = Map.of("STUDENT", 0.15, "SENIOR", 0.30);
        var req = new FareRuleCreateRequest(1L, 1L, 3L, new BigDecimal("45000.00"), discounts, FareRule.DynamicPricing.ON);
        var resp = fareRuleResponse(1L, 1L, 1L, 3L, new BigDecimal("45000.00"), discounts);

        when(service.createFareRule(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/fare-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/fare-rules/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.basePrice").value(45000.00))
                .andExpect(jsonPath("$.route.code").value("SM-BAQ-01"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        var discounts = Map.of("STUDENT", 0.15);
        when(service.getFareRule(3L))
                .thenReturn(fareRuleResponse(3L, 1L, 1L, 3L, new BigDecimal("45000.00"), discounts));

        mvc.perform(get("/api/v1/fare-rules/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.basePrice").value(45000.00));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        var fareRules = List.of(
                fareRuleResponse(1L, 1L, 1L, 3L, new BigDecimal("45000.00"), Map.of("STUDENT", 0.15)),
                fareRuleResponse(2L, 1L, 1L, 2L, new BigDecimal("25000.00"), Map.of("SENIOR", 0.30))
        );

        when(service.getAllFareRules()).thenReturn(fareRules);

        mvc.perform(get("/api/v1/fare-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByRoute_shouldReturn200() throws Exception {
        var fareRules = List.of(
                fareRuleResponse(1L, 1L, 1L, 3L, new BigDecimal("45000.00"), Map.of("STUDENT", 0.15))
        );

        when(service.getFareRulesByRouteId(1L)).thenReturn(fareRules);

        mvc.perform(get("/api/v1/fare-rules/by-route/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void calculatePrice_shouldReturn200() throws Exception {
        when(service.calculatePrice(1L, 1L, 3L, 5L, 1L, "A12", 10L))
                .thenReturn(new BigDecimal("38250.00"));

        mvc.perform(get("/api/v1/fare-rules/calculate-price")
                        .param("routeId", "1")
                        .param("fromStopId", "1")
                        .param("toStopId", "3")
                        .param("passengerId", "5")
                        .param("busId", "1")
                        .param("seatNumber", "A12")
                        .param("tripId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(38250.00));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new FareRuleUpdateRequest(new BigDecimal("50000.00"), Map.of("SENIOR", 0.25), FareRule.DynamicPricing.OFF);

        mvc.perform(patch("/api/v1/fare-rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateFareRule(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/fare-rules/1"))
                .andExpect(status().isNoContent());

        verify(service).deleteFareRule(1L);
    }

    // Helper
    private FareRuleResponse fareRuleResponse(Long id, Long routeId, Long fromStopId, Long toStopId,
                                              BigDecimal basePrice, Map<String, Double> discounts) {
        var routeSummary = new FareRuleResponse.RouteSummary(routeId, "SM-BAQ-01", "Santa Marta - Barranquilla");
        var fromStopSummary = new FareRuleResponse.StopSummary(fromStopId, "Santa Marta", 1);
        var toStopSummary = new FareRuleResponse.StopSummary(toStopId, "Barranquilla", 3);

        return new FareRuleResponse(id, routeSummary, fromStopSummary, toStopSummary,
                basePrice, discounts, "ON");
    }
}