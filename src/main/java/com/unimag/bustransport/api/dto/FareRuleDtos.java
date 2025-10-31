package com.unimag.bustransport.api.dto;

import com.unimag.bustransport.domain.entities.FareRule;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

public class FareRuleDtos {
    public record FareRuleCreateRequest(
            @NotNull Long routeId,
            @NotNull Long fromStopId,
            @NotNull Long toStopId,
            @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0")
            @NotNull BigDecimal basePrice,
            Map<@Pattern(regexp = "^(CHILD|STUDENT|SENIOR)$") String,
            @Min(value = 0) @Max(value = 100) Double>  discounts,
            @NotNull FareRule.DinamyPricing dinamycPricing
            ) implements Serializable {}
    public record FareRuleUpdateRequest(
            BigDecimal basePrice,
            Map<String, Double> discounts,
            FareRule.DinamyPricing dinamycPricing
    ) implements Serializable {}
    public record FareRuleResponse(
            Long id,
            Long routeId,
            StopSummary fromStop,
            StopSummary toStop,
            BigDecimal basePrice,
            Map<String, Double> discounts,
            String dinamycPricing
    ) implements Serializable {}

    public record StopSummary(Long id, String name, Integer order) implements Serializable {}

}
