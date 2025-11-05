package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PassengerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "user", ignore = true)
    Passenger toEntity(PassengerDtos.PassengerCreateRequest request);


    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "documentType", target = "documentType")
    @Mapping(source = "documentNumber", target = "documentNumber")
    @Mapping(source = "birthDate", target = "birthDate")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    PassengerDtos.PassengerResponse toResponse(Passenger passenger);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(PassengerDtos.PassengerUpdateRequest request, @MappingTarget Passenger passenger);
}

