package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.domain.entities.Passenger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FareRuleService {

    FareRuleDtos.FareRuleResponse createFareRule(FareRuleDtos.FareRuleCreateRequest request);

    void updateFareRule(Long id, FareRuleDtos.FareRuleUpdateRequest request);

    void deleteFareRule(Long id);

    FareRuleDtos.FareRuleResponse getFareRule(Long id);

    List<FareRuleDtos.FareRuleResponse> getAllFareRules();

    List<FareRuleDtos.FareRuleResponse> getFareRulesByRouteId(Long routeId);

    BigDecimal calculatePrice(Long routeId, Long fromStopId, Long toStopId,
                              Long passengerId, Long busId, String seatNumber, Long TripId);
}
