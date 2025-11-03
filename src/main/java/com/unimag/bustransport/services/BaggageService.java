package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.BaggageDtos;

import java.math.BigDecimal;
import java.util.List;

public interface BaggageService {
    BaggageDtos.BaggageResponse registerBaggage(BaggageDtos.BaggageCreateRequest request);
    void updateBaggage(BaggageDtos.BaggageUpdateRequest request);
    List<BaggageDtos.BaggageResponse> getBaggageByTicket(Long ticketId);
    BigDecimal calculateBaggageFee(double weightKg);
}
