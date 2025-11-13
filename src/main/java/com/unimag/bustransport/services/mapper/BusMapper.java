package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.domain.entities.Bus;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface BusMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trips", ignore = true)
    @Mapping(target = "seats", ignore = true)
    @Mapping(source = "status", target = "status")
    Bus toEntity(BusDtos.BusCreateRequest request);

    @Mapping(source = "status", target = "status")
    @Mapping(target = "totalSeats", expression = "java(bus.getSeats() != null ? bus.getSeats().size() : 0)")
    BusDtos.BusResponse toResponse(Bus bus);

    @Mapping(source = "status", target = "status")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BusDtos.BusUpdateRequest request, @MappingTarget Bus bus);
}
