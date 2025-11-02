package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.FareRule;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FareRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    @Mapping(source = "dinamycPricing", target = "dinamyPricing")
    FareRule toEntity(FareRuleDtos.FareRuleCreateRequest request);

    @Mapping(source = "dinamyPricing", target = "dinamycPricing")
    @Mapping(source = "route.id", target = "routeId")
    @Mapping(source = "fromStop.id", target = "fromStop.id")
    @Mapping(source = "fromStop.name", target = "fromStop.name")
    @Mapping(source = "fromStop.order", target = "fromStop.order")
    @Mapping(source = "toStop.id", target = "toStop.id")
    @Mapping(source = "toStop.name", target = "toStop.name")
    @Mapping(source = "toStop.order", target = "toStop.order")
    FareRuleDtos.FareRuleResponse toResponse(FareRule fareRule);

    @Mapping(source = "dinamycPricing", target = "dinamyPricing")
    void updateEntityFromRequest(FareRuleDtos.FareRuleUpdateRequest request, @MappingTarget FareRule fareRule);
}
