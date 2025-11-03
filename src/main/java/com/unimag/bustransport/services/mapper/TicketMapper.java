package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.Ticket;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "fromStop", ignore = true)
    @Mapping(target = "toStop", ignore = true)
    @Mapping(target = "purchase", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "qrCode", ignore = true)
    Ticket toEntity(TicketDtos.TicketCreateRequest request);

    @Mapping(target = "status", expression = "java(ticket.getStatus().toString())")
    @Mapping(source = "trip.id", target = "trip.id")
    @Mapping(source = "trip.route.origin", target = "trip.origin")
    @Mapping(source = "trip.route.destination", target = "trip.destination")
    @Mapping(source = "trip.departureAt", target = "trip.departureAt")
    @Mapping(source = "passenger.id", target = "passenger.id")
    @Mapping(source = "passenger.fullName", target = "passenger.fullName")
    @Mapping(source = "passenger.documentNumber", target = "passenger.documentNumber")
    @Mapping(source = "fromStop.id", target = "fromStop.id")
    @Mapping(source = "fromStop.name", target = "fromStop.name")
    @Mapping(source = "fromStop.city", target = "fromStop.city")
    @Mapping(source = "toStop.id", target = "toStop.id")
    @Mapping(source = "toStop.name", target = "toStop.name")
    @Mapping(source = "toStop.city", target = "toStop.city")
    @Mapping(source = "purchase.id", target = "purchase.id")
    @Mapping(source = "purchase.totalAmount", target = "purchase.totalAmount")
    @Mapping(target = "purchase.paymentStatus", expression = "java(ticket.getPurchase().getPaymentStatus().toString())")
    TicketDtos.TicketResponse toResponse(Ticket ticket);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", expression = "java(request.status() != null ? com.unimag.bustransport.domain.entities.Ticket.Status.valueOf(request.status()) : null)")
    void updateEntityFromRequest(TicketDtos.TicketUpdateRequest request, @MappingTarget Ticket ticket);
}
