package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import com.unimag.bustransport.services.FareRuleService;

import java.math.BigDecimal;
import java.util.List;

public class FareRuleServiceImpl implements FareRuleService {
    @Override
    public FareRuleDtos.FareRuleResponse createFareRule(FareRuleDtos.FareRuleCreateRequest request) {
        return null;
    }

    @Override
    public void updateFareRule(Long id, FareRuleDtos.FareRuleUpdateRequest request) {

    }

    @Override
    public void deleteFareRule(Long id) {

    }

    @Override
    public FareRuleDtos.FareRuleResponse getFareRule(Long id) {
        return null;
    }

    @Override
    public BigDecimal calculateFarePrice(Long routeId, Long fromId, Long toId, Passenger passenger) {
        return null;
    }

    @Override
    public List<FareRuleDtos.FareRuleResponse> getFareRulesByRouteId(Long routeId) {
        return List.of();
    }
}
