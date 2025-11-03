package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.FareRule;
import com.unimag.bustransport.domain.entities.Passenger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FareRuleService {
    FareRuleDtos.FareRuleResponse createFareRule(FareRuleDtos.FareRuleCreateRequest  request);
    void updateFareRule(Long id,FareRuleDtos.FareRuleUpdateRequest  request);
    void deleteFareRule(Long id);
    FareRuleDtos.FareRuleResponse getFareRule(Long id);
    BigDecimal calculateFarePrice(Long routeId, Long fromId, Long toId, Passenger passenger);
    List<FareRuleDtos.FareRuleResponse> getFareRulesByRouteId(Long routeId);
}
