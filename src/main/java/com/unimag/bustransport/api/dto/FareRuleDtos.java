package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.FareRule;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

public class FareRuleDtos {
    public record FareRuleCreateRequest(
            @NotNull Long routeId,
            @NotNull Long fromStopId,
            @NotNull Long toStopId,
            @NotNull BigDecimal basePrice,
            Map<String, Double> discounts,
            @NotNull FareRule.DinamyPricing dinamyPricing
            ) implements Serializable {}
    public record FareRuleUpdateRequest(
            BigDecimal basePrice,
            Map<String, Double> discounts,
            FareRule.DinamyPricing dynamicPricing
    ) implements Serializable {}
    public record FareRuleResponse(
            Long id,
            Long routeId,
            StopSummary fromStop,
            StopSummary toStop,
            BigDecimal basePrice,
            Map<String, Double> discounts,
            String dynamicPricing
    ) implements Serializable {}

    public record StopSummary(Long id, String name, Integer order) implements Serializable {}

}
