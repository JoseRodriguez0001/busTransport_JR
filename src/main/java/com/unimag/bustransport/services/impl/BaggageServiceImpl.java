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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BaggageServiceImpl implements BaggageService {

    private final BaggageRepository baggageRepository;
    private final TicketRepository ticketRepository;
    private final BaggageMapper baggageMapper;
    private final ConfigService configService;

    @Override
    public BaggageDtos.BaggageResponse registerBaggage(BaggageDtos.BaggageCreateRequest request) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new NotFoundException(String.format("Ticket with ID %d not found", request.ticketId())));

        if (ticket.getStatus() != Ticket.Status.SOLD) {
            throw new IllegalStateException(
                    String.format("Cannot register baggage for ticket with status %s. Only SOLD tickets allowed.",
                            ticket.getStatus())
            );
        }

        // Validación 1: Peso positivo
        if (request.weightKg() <= 0) {
            throw new IllegalArgumentException("Baggage weight must be greater than 0");
        }

        // Validación 2: Peso máximo por maleta
        BigDecimal maxWeightPerBag = configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG");
        if (BigDecimal.valueOf(request.weightKg()).compareTo(maxWeightPerBag) > 0) {
            throw new IllegalArgumentException(
                    String.format("Baggage exceeds maximum weight per bag (%.2f kg)", maxWeightPerBag)
            );
        }

        // Validación 3: Límite de equipajes por ticket
        int currentBaggageCount = baggageRepository.countByTicketId(ticket.getId());
        int maxBaggagePerTicket = configService.getValueAsInt("MAX_BAGGAGE_PER_TICKET");
        if (currentBaggageCount >= maxBaggagePerTicket) {
            throw new IllegalStateException(
                    String.format("Ticket already has maximum allowed baggage (%d)", maxBaggagePerTicket)
            );
        }

        BigDecimal fee = calculateBaggageFee(request.weightKg());

        Baggage baggage = baggageMapper.toEntity(request);
        baggage.setTicket(ticket);
        baggage.setFee(fee);
        baggage.setTagCode(generateTagCode(ticket.getId()));

        baggageRepository.save(baggage);
        log.info("Baggage registered with ID {} for ticket {}", baggage.getId(), ticket.getId());
        return baggageMapper.toResponse(baggage);
    }

    @Override
    public void updateBaggage(Long id, BaggageDtos.BaggageUpdateRequest request){

        Baggage baggage = baggageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Baggage with ID %d not found", id)));

        if (request.weightKg() != null) {
            // Validación 1: Peso positivo
            if (request.weightKg() <= 0) {
                throw new IllegalArgumentException("Baggage weight must be greater than 0");
            }

            // Validación 2: Peso máximo por maleta
            BigDecimal maxWeightPerBag = configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG");
            if (BigDecimal.valueOf(request.weightKg()).compareTo(maxWeightPerBag) > 0) {
                throw new IllegalArgumentException(
                        String.format("Baggage exceeds maximum weight per bag (%.2f kg)", maxWeightPerBag)
                );
            }

            baggage.setWeightKg(request.weightKg());
            baggage.setFee(calculateBaggageFee(request.weightKg()));
        }

        if (request.tagCode() != null) {
            baggage.setTagCode(request.tagCode());
        }

        baggageRepository.save(baggage);
        log.info("Baggage with ID {} updated", id);
    }

    @Override
    public List<BaggageDtos.BaggageResponse> getBaggageByTicket(Long ticketId){
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException(String.format("Ticket with ID %d not found", ticketId)));

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
                .orElseThrow(() -> new NotFoundException(String.format("Baggage with ID %d not found", id)));
        baggageRepository.delete(baggage);
        log.info("Baggage with ID {} deleted", id);
    }

    private String generateTagCode(Long ticketId) {
        return String.format("BAG-%08d-%d", ticketId, System.currentTimeMillis() % 100000);
    }
}
