package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.ParcelDtos;
import com.unimag.bustransport.domain.entities.Parcel;
import org.mapstruct.*;

@Mapper (componentModel = "spring")
public interface ParcelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "proofPhotoUrl", ignore = true)
    @Mapping(target = "deliveryOtp", ignore = true)
    Parcel toEntity(ParcelDtos.ParcelCreateRequest request);

    @Mapping(target = "status", expression = "java(parcel.getStatus().toString())")
    @Mapping(source = "trip.id", target = "trip.id")
    @Mapping(source = "trip.route.origin", target = "trip.origin")
    @Mapping(source = "trip.route.destination", target = "trip.destination")
    @Mapping(source = "fromStop.id", target = "fromStop.id")
    @Mapping(source = "fromStop.name", target = "fromStop.name")
    @Mapping(source = "toStop.id", target = "toStop.id")
    @Mapping(source = "toStop.name", target = "toStop.name")
    ParcelDtos.ParcelResponse toResponse(Parcel parcel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", expression = "java(request.status() != null ? com.unimag.bustransport.domain.entities.Parcel.Status.valueOf(request.status()) : null)")
    void updateEntityFromRequest(ParcelDtos.ParcelUpdateRequest request, @MappingTarget Parcel parcel);
}
