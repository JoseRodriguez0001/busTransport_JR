package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.RouteDtos;
import com.unimag.bustransport.domain.entities.Route;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {StopMapper.class, FareRuleMapper.class})
public interface RouteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "trips", ignore = true)
    @Mapping(target = "fareRules", ignore = true)
    Route toEntity(RouteDtos.RouteCreateRequest request);

    RouteDtos.RouteResponse toResponse(Route route);

    void updateEntityFromRequest(RouteDtos.RouteUpdateRequest request, @MappingTarget Route route);
}
