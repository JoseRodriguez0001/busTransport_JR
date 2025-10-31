package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.domain.entities.Trip;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "route", ignore = true)
    @Mapping(target = "bus", ignore = true)
    @Mapping(target = "status", ignore = true)
    Trip toEntity(TripDtos.TripCreateRequest request);

    @Mapping(target = "status", expression = "java(trip.getStatus().toString())")
    @Mapping(target = "durationMinutes", expression = "java(java.time.temporal.ChronoUnit.MINUTES.between(trip.getDepartureAt(), trip.getArrivalAt()).intValue())")
    @Mapping(source = "route.id", target = "route.id")
    @Mapping(source = "route.code", target = "route.code")
    @Mapping(source = "route.origin", target = "route.origin")
    @Mapping(source = "route.destination", target = "route.destination")
    @Mapping(source = "bus.id", target = "bus.id")
    @Mapping(source = "bus.plate", target = "bus.plate")
    @Mapping(source = "bus.capacity", target = "bus.capacity")
    @Mapping(target = "soldSeats", ignore = true)
    @Mapping(target = "availableSeats", ignore = true)
    TripDtos.TripResponse toResponse(Trip trip);

    void updateEntityFromRequest(TripDtos.TripUpdateRequest request, @MappingTarget Trip trip);
}
