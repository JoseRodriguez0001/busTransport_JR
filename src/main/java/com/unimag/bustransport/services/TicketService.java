package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.api.dto.TicketDtos;

import java.time.LocalDate;
import java.util.List;

public interface TicketService {
    TicketDtos.TicketResponse createTicket(TicketDtos.TicketCreateRequest request);
    void deleteTicket(Long id);
    TicketDtos.TicketResponse getTicket(Long id);
    List<TicketDtos.TicketResponse> getTicketsByTrip(Long tripId);
    List<TicketDtos.TicketResponse> getTicketsByPurchase(Long purchaseId);
    List<TicketDtos.TicketResponse> getTicketsByPassenger(Long passengerId);
    TicketDtos.TicketResponse assignPassenger(Long ticketId, Long passengerId);//no se si es mejor pasar el pasajero y no el id
    void markCheckedIn(Long ticketId);
    void generateQrForTicket(Long ticketId);
    void validateQrForTicket(String qrCode);
}
