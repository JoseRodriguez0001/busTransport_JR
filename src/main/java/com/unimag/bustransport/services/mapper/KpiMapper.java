package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.KpiDtos;
import com.unimag.bustransport.domain.entities.Kpi;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface KpiMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "calculatedAt", ignore = true)
    Kpi toEntity(KpiDtos.KpiCreateRequest request);

    KpiDtos.KpiResponse toResponse(Kpi kpi);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(KpiDtos.KpiUpdateRequest request, @MappingTarget Kpi kpi);
}
