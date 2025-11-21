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
            @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
            @NotNull BigDecimal basePrice,
            Map<String, @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") Double> discounts,
            @NotNull FareRule.DynamicPricing dynamicPricing
    ) implements Serializable {}

    public record FareRuleUpdateRequest(
            @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
            BigDecimal basePrice,
            Map<String, @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") Double> discounts,
            FareRule.DynamicPricing dynamicPricing
    ) implements Serializable {}

    public record FareRuleResponse(
            Long id,
            RouteSummary route,
            StopSummary fromStop,
            StopSummary toStop,
            BigDecimal basePrice,
            Map<String, Double> discounts,
            String dynamicPricing
    ) implements Serializable {
        public record RouteSummary(Long id, String code, String name) implements Serializable {}
        public record StopSummary(Long id, String name, Integer order) implements Serializable {}
    }

    public record PriceResponse(BigDecimal price) implements Serializable {}
}
