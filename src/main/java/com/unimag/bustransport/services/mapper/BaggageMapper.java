package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.BaggageDtos;
import com.unimag.bustransport.domain.entities.Baggage;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BaggageMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    Baggage toEntity(BaggageDtos.BaggageCreateRequest request);

    @Mapping(source = "ticket.id", target = "ticket.id")
    @Mapping(source = "ticket.seatNumber", target = "ticket.seatNumber")
    @Mapping(source = "ticket.qrCode", target = "ticket.qrCode")
    BaggageDtos.BaggageResponse toResponse(Baggage baggage);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BaggageDtos.BaggageUpdateRequest request, @MappingTarget Baggage baggage);
}
