package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.domain.entities.Stop;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface FareRuleMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    FareRule toEntity(FareRuleDtos.FareRuleCreateRequest request);

    @Mapping(target = "routeId", source = "route.id")
    @Mapping(target = "fromStop", source = "fromStop", qualifiedByName = "toStopSummary")
    @Mapping(target = "toStop", source = "toStop", qualifiedByName = "toStopSummary")
    @Mapping(target = "dinamycPricing", expression = "java(fareRule.getDinamycPricing().toString())")
    FareRuleDtos.FareRuleResponse toResponse(FareRule fareRule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    void updateEntityFromRequest(FareRuleDtos.FareRuleUpdateRequest request, @MappingTarget FareRule fareRule);

    @Named("toStopSummary")
    default FareRuleDtos.StopSummary toStopSummary(Stop stop) {
        if (stop == null) {
            return null;
        }
        return new FareRuleDtos.StopSummary(
                stop.getId(),
                stop.getName(),
                stop.getOrder()
        );
    }
}