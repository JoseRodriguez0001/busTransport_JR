package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.domain.entities.Route;
import com.unimag.bustransport.domain.entities.Stop;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FareRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    FareRule toEntity(FareRuleDtos.FareRuleCreateRequest request);

    @Mapping(target = "route", source = "route", qualifiedByName = "toRouteSummary")
    @Mapping(target = "fromStop", source = "fromStop", qualifiedByName = "toStopSummary")
    @Mapping(target = "toStop", source = "toStop", qualifiedByName = "toStopSummary")
    @Mapping(target = "dynamicPricing", source = "dynamicPricing", qualifiedByName = "enumToString")
    FareRuleDtos.FareRuleResponse toResponse(FareRule fareRule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    void updateEntityFromRequest(FareRuleDtos.FareRuleUpdateRequest request, @MappingTarget FareRule fareRule);

    @Named("toRouteSummary")
    default FareRuleDtos.FareRuleResponse.RouteSummary toRouteSummary(Route route) {
        if (route == null) {
            return null;
        }
        return new FareRuleDtos.FareRuleResponse.RouteSummary(
                route.getId(),
                route.getCode(),
                route.getName()
        );
    }

    @Named("toStopSummary")
    default FareRuleDtos.FareRuleResponse.StopSummary toStopSummary(Stop stop) {
        if (stop == null) {
            return null;
        }
        return new FareRuleDtos.FareRuleResponse.StopSummary(
                stop.getId(),
                stop.getName(),
                stop.getOrder()
        );
    }

    @Named("enumToString")
    default String enumToString(FareRule.DynamicPricing dynamicPricing) {
        return dynamicPricing != null ? dynamicPricing.name() : null;
    }
}
