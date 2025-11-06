package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.services.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    @Override
    public TicketDtos.TicketResponse createTicket(TicketDtos.TicketCreateRequest request) {
        return null;
    }

    @Override
    public void deleteTicket(Long id) {

    }

    @Override
    public TicketDtos.TicketResponse getTicket(Long id) {
        return null;
    }

    @Override
    public List<TicketDtos.TicketResponse> getTicketsByTrip(Long tripId) {
        return List.of();
    }

    @Override
    public List<TicketDtos.TicketResponse> getTicketsByPurchase(Long purchaseId) {
        return List.of();
    }

    @Override
    public List<TicketDtos.TicketResponse> getTicketsByPassenger(Long passengerId) {
        return List.of();
    }

    @Override
    public TicketDtos.TicketResponse assignPassenger(Long ticketId, Long passengerId) {
        return null;
    }

    @Override
    public void markCheckedIn(Long ticketId) {

    }

    @Override
    public void generateQrForTicket(Long ticketId) {

    }

    @Override
    public void validateQrForTicket(String qrCode) {

    }
}
