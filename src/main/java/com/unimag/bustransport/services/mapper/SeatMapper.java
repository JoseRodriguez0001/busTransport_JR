package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.domain.entities.Seat;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bus", ignore = true)
    Seat toEntity(SeatDtos.SeatCreateRequest request);

    @Mapping(target = "type", expression = "java(seat.getType().toString())")
    @Mapping(source = "bus.id", target = "busId")
    @Mapping(source = "bus.plate", target = "busPlate")
    @Mapping(target = "occupied", ignore = true)
    SeatDtos.SeatResponse toResponse(Seat seat);

    void updateEntityFromRequest(SeatDtos.SeatUpdateRequest request, @MappingTarget Seat seat);
}
