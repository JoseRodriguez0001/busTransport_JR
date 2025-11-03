package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.ConfigDtos;
import com.unimag.bustransport.domain.entities.Config;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ConfigMapper {
    @Mapping(target = "id", ignore = true)
    Config toEntity(ConfigDtos.ConfigCreateRequest request);

    ConfigDtos.ConfigResponse toResponse(Config config);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ConfigDtos.ConfigUpdateRequest request, @MappingTarget Config config);
}
