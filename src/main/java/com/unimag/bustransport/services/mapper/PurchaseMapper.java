package com.unimag.bustransport.services.mapper;

import com.unimag.bustransport.api.dto.PurchaseDtos;
import com.unimag.bustransport.domain.entities.Purchase;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "paymentStatus", constant = "PENDING")  // valor por defecto
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    Purchase toEntity(PurchaseDtos.PurchaseCreateRequest request);

    @Mapping(source = "user.id", target = "user.id")
    @Mapping(source = "user.username", target = "user.username")
    @Mapping(source = "user.email", target = "user.email")
    @Mapping(target = "tickets", expression =
            "java(purchase.getTickets().stream().map(t -> " +
                    "new PurchaseDtos.PurchaseResponse.TicketSummary(t.getId(), t.getSeatNumber(), t.getPrice(), t.getStatus().name()))" +
                    ".toList())")
    PurchaseDtos.PurchaseResponse toResponse(Purchase purchase);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(PurchaseDtos.PurchaseUpdateRequest request, @MappingTarget Purchase purchase);
}
