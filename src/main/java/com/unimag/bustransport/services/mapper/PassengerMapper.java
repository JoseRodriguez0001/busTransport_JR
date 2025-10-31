package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface PassengerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(source = "fullname", target = "fullName")
    @Mapping(target = "createAt", ignore = true)
    Passenger toEntity(PassengerDtos.PassengerCreateRequest request);

    @Mapping(source = "fullName", target = "fullname")
    PassengerDtos.PassengerResponse toResponse(Passenger passenger);

    @Mapping(source = "fullname", target = "fullName")
    void updateEntityFromRequest(PassengerDtos.PassengerUpdateRequest request, @MappingTarget Passenger passenger);
}
