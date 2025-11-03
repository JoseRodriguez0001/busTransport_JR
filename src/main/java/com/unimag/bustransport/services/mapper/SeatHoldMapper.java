package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.SeatHoldDtos;
import com.unimag.bustransport.domain.entities.SeatHold;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SeatHoldMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    SeatHold toEntity(SeatHoldDtos.SeatHoldCreateRequest request);

    @Mapping(target = "status", expression = "java(seatHold.getStatus().toString())")
    @Mapping(source = "trip.id", target = "trip.id")
    @Mapping(source = "trip.route.origin", target = "trip.origin")
    @Mapping(source = "trip.route.destination", target = "trip.destination")
    @Mapping(source = "trip.departureAt", target = "trip.departureAt")
    @Mapping(source = "user.id", target = "user.id")
    @Mapping(source = "user.username", target = "user.username")
    SeatHoldDtos.SeatHoldResponse toResponse(SeatHold seatHold);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", expression = "java(request.status() != null ? com.unimag.bustransport.domain.entities.SeatHold.Status.valueOf(request.status()) : null)")
    void updateEntityFromRequest(SeatHoldDtos.SeatHoldUpdateRequest request, @MappingTarget SeatHold seatHold);
}
