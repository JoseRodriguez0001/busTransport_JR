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
    @Mapping(target = "seatHolds", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "parcels", ignore = true)
    Trip toEntity(TripDtos.TripCreateRequest request);

    @Mapping(target = "status", expression = "java(trip.getStatus().toString())")
    @Mapping(target = "durationMinutes", expression = "java(java.time.temporal.ChronoUnit.MINUTES.between(trip.getDepartureAt(), trip.getArrivalAt()).intValue())")
    @Mapping(target = "route", source = "route")
    @Mapping(target = "bus", source = "bus")
    @Mapping(target = "soldSeats", ignore = true)
    @Mapping(target = "availableSeats", ignore = true)
    TripDtos.TripResponse toResponse(Trip trip);


    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "origin", source = "origin")
    @Mapping(target = "destination", source = "destination")
    TripDtos.TripResponse.RouteSummary toRouteSummary(com.unimag.bustransport.domain.entities.Route route);


    @Mapping(target = "id", source = "id")
    @Mapping(target = "plate", source = "plate")
    @Mapping(target = "capacity", source = "capacity")
    TripDtos.TripResponse.BusSummary toBusSummary(com.unimag.bustransport.domain.entities.Bus bus);

    @Mapping(target = "route", ignore = true)
    @Mapping(target = "bus", ignore = true)
    @Mapping(target = "seatHolds", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "parcels", ignore = true)
    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(TripDtos.TripUpdateRequest request, @MappingTarget Trip trip);
}

