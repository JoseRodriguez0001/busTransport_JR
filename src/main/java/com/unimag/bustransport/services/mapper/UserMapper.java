package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PassengerMapper.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserDtos.UserCreateRequest request);

    @Mapping(target = "createAt", source = "createdAt")
    UserDtos.UserResponse toResponse(User user);

    @Mapping(source = "password", target = "passwordHash")
    void updateEntityFromRequest(UserDtos.UserUpdateRequest request, @MappingTarget User user);
}
