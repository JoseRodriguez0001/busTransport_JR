package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BaggageDtos;
import com.unimag.bustransport.domain.entities.Baggage;
import com.unimag.bustransport.domain.entities.Ticket;
import com.unimag.bustransport.domain.repositories.BaggageRepository;
import com.unimag.bustransport.domain.repositories.TicketRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.BaggageService;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.BaggageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor        //Genera un constructor con todos los campos que sean "final" en esta clase
@Transactional
public class BaggageServiceImpl implements BaggageService {

    //dependencias que quiero que spring inyecte aqui
    private final BaggageRepository baggageRepository;
    private final TicketRepository ticketRepository;
    private final BaggageMapper baggageMapper;
    private final ConfigService configService; //revisar por que se llama a la interfaz y no a la implementacion

    @Override
    public BaggageDtos.BaggageResponse registerBaggage(BaggageDtos.BaggageCreateRequest request) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new NotFoundException("Ticket no encontrado"));

        // VALIDAR que el ticket esté SOLD
        if (ticket.getStatus() != Ticket.Status.SOLD) {
            throw new IllegalStateException(
                    String.format("Cannot register baggage for ticket with status %s. Only SOLD tickets allowed.",
                            ticket.getStatus())
            );
        }
        BigDecimal fee = calculateBaggageFee(request.weightKg());

        Baggage baggage = baggageMapper.toEntity(request);
        baggage.setTicket(ticket);
        baggage.setFee(fee);
        baggage.setTagCode(generateTagCode(ticket.getId()));

        baggageRepository.save(baggage);
        return baggageMapper.toResponse(baggage);
    }

    @Override
    public void updateBaggage(Long id, BaggageDtos.BaggageUpdateRequest request){

        Baggage baggage = baggageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipaje no encontrado"));

        if (request.weightKg() != null) {
            baggage.setWeightKg(request.weightKg());
            baggage.setFee(calculateBaggageFee(request.weightKg()));
        }

        if (request.tagCode() != null) {
            baggage.setTagCode(request.tagCode());
        }

        baggageRepository.save(baggage);

    }

    @Override
    public List<BaggageDtos.BaggageResponse> getBaggageByTicket(Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket no encontrado"));

        List<Baggage> baggages = baggageRepository.findByTicketId(ticketId);

        return baggages.stream().map(baggageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateBaggageFee(double weightKg){
        BigDecimal freeLimit = configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
        BigDecimal feePerKg = configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG");

        BigDecimal weight = BigDecimal.valueOf(weightKg);

        if (weight.compareTo(freeLimit) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excess = weight.subtract(freeLimit);
        return excess.multiply(feePerKg);
    }

    @Override
    public void deleteBaggage(Long id){
        Baggage baggage = baggageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipaje no encontrado"));
        baggageRepository.delete(baggage);
    }


    private String generateTagCode(Long ticketId) {
        return String.format("BAG-%08d-%d", ticketId, System.currentTimeMillis() % 100000);
    }
}
