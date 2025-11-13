package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.domain.entities.Assignment;
import com.unimag.bustransport.api.dto.AssignmentDtos;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    //quiere decir "no intentes llenar ese atributo de la entidad"
    //target -> objeto destino / source -> objeto fuente u objeto origen
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip.id", source = "tripId")
    @Mapping(target = "driver.id", source = "driverId")
    @Mapping(target = "dispatcher.id", source = "dispatcherId")
    @Mapping(target = "assignedAt", ignore = true)
    Assignment toEntity(AssignmentDtos.AssignmentCreateRequest request);

    @Mapping(source = "trip.id", target = "trip.id")
    @Mapping(source = "trip.route.origin", target = "trip.origin")
    @Mapping(source = "trip.route.destination", target = "trip.destination")
    @Mapping(source = "trip.departureAt", target = "trip.departureAt")
    @Mapping(source = "driver.id", target = "driver.id")
    @Mapping(source = "driver.name", target = "driver.name")
    @Mapping(source = "dispatcher.id", target = "dispatcher.id")
    @Mapping(source = "dispatcher.name", target = "dispatcher.name")
    AssignmentDtos.AssignmentResponse toResponse(Assignment assignment);

    //“Si en el objeto de origen (source) hay un campo con valor null,
    //no lo copies al objeto destino (target). Déjalo tal como estaba.”
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(AssignmentDtos.AssignmentUpdateRequest request, @MappingTarget Assignment assignment);
}
