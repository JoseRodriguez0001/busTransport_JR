package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PassengerMapper.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "assignmentsAsDriver", ignore = true)
    @Mapping(target = "assignmentsAsDispatcher", ignore = true)
    @Mapping(target = "seatHolds", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    User toEntity(UserDtos.UserCreateRequest request);

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "assignmentsAsDriver", ignore = true)
    @Mapping(target = "assignmentsAsDispatcher", ignore = true)
    @Mapping(target = "seatHolds", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    UserDtos.UserResponse toResponse(User user);

    @Mapping(source = "password", target = "passwordHash")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignmentsAsDriver", ignore = true)
    @Mapping(target = "assignmentsAsDispatcher", ignore = true)
    @Mapping(target = "seatHolds", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    void updateEntityFromRequest(UserDtos.UserUpdateRequest request, @MappingTarget User user);
}
