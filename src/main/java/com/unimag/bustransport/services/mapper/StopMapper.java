package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.StopDtos;
import com.unimag.bustransport.domain.entities.Stop;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StopMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "fareRulesFrom", ignore = true)
    @Mapping(target = "fareRulesTo", ignore = true)
    Stop toEntity(StopDtos.StopCreateRequest request);

    StopDtos.StopResponse toResponse(Stop stop);

    void updateEntityFromRequest(StopDtos.StopUpdateRequest request, @MappingTarget Stop stop);
}
