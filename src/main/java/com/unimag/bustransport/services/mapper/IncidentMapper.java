package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.domain.entities.Incident;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface IncidentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "entityType", expression = "java(com.unimag.bustransport.domain.entities.Incident.EntityType.valueOf(request.entityType()))")
    @Mapping(target = "type", expression = "java(com.unimag.bustransport.domain.entities.Incident.Type.valueOf(request.type()))")
    Incident toEntity(IncidentDtos.IncidentCreateRequest request);

    @Mapping(target = "entityType", expression = "java(incident.getEntityType().toString())")
    @Mapping(target = "type", expression = "java(incident.getType().toString())")
    IncidentDtos.IncidentResponse toResponse(Incident incident);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "type", expression = "java(request.type() != null ? com.unimag.bustransport.domain.entities.Incident.Type.valueOf(request.type()) : null)")
    void updateEntityFromRequest(IncidentDtos.IncidentUpdateRequest request, @MappingTarget Incident incident);
}
