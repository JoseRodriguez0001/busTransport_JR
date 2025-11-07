package com.unimag.bustransport.services.impl;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.FareRuleService;
import com.unimag.bustransport.services.mapper.FareRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FareRuleServiceImpl implements FareRuleService {
    private final FareRuleRepository fareRuleRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final PassengerRepository passengerRepository;
    private final SeatRepository seatRepository;
    private final FareRuleMapper fareRuleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public FareRuleDtos.FareRuleResponse createFareRule(FareRuleDtos.FareRuleCreateRequest request) {
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Route with ID %d not found", request.routeId())
                ));

        Stop fromStop = stopRepository.findById(request.fromStopId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Stop with ID %d not found", request.fromStopId())
                ));

        Stop toStop = stopRepository.findById(request.toStopId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Stop with ID %d not found", request.toStopId())
                ));

        validateStopsBelongToRoute(route.getId(), fromStop.getId(), toStop.getId());
        validateStopOrder(fromStop, toStop);

        FareRule fareRule = fareRuleMapper.toEntity(request);
        fareRule.setRoute(route);
        fareRule.setFromStop(fromStop);
        fareRule.setToStop(toStop);

        fareRuleRepository.save(fareRule);

        log.info("Fare rule created with ID {} for route {} from stop {} to stop {}",
                fareRule.getId(), route.getId(), fromStop.getId(), toStop.getId());

        return fareRuleMapper.toResponse(fareRule);
    }

    @Override
    public void updateFareRule(Long id, FareRuleDtos.FareRuleUpdateRequest request) {
        FareRule fareRule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Fare rule with ID %d not found", id)
                ));

        if (request.basePrice() != null) {
            fareRule.setBasePrice(request.basePrice());
        }

        if (request.discounts() != null) {
            fareRule.setDiscounts(request.discounts());
        }

        if (request.dynamicPricing() != null) {
            fareRule.setDynamycPricing(FareRule.DynamycPricing.valueOf(String.valueOf(request.dynamicPricing()));
        }

        fareRuleRepository.save(fareRule);

        log.info("Fare rule with ID {} updated", id);
    }

    @Override
    public void deleteFareRule(Long id) {
        FareRule fareRule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Fare rule with ID %d not found", id)
                ));

        fareRuleRepository.delete(fareRule);

        log.info("Fare rule with ID {} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public FareRuleDtos.FareRuleResponse getFareRule(Long id) {
        FareRule fareRule = fareRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Fare rule with ID %d not found", id)
                ));

        log.info("Fare rule with ID {} retrieved", id);

        return fareRuleMapper.toResponse(fareRule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareRuleDtos.FareRuleResponse> getAllFareRules() {
        List<FareRule> fareRules = fareRuleRepository.findAll();

        log.info("Retrieved {} fare rules", fareRules.size());

        return fareRules.stream()
                .map(fareRuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareRuleDtos.FareRuleResponse> getFareRulesByRouteId(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new NotFoundException(String.format("Route with ID %d not found", routeId));
        }

        List<FareRule> fareRules = fareRuleRepository.findByRouteId(routeId);

        log.info("Retrieved {} fare rules for route ID {}", fareRules.size(), routeId);

        return fareRules.stream()
                .map(fareRuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(Long routeId, Long fromStopId, Long toStopId,
                                     Long passengerId, Long busId, String seatNumber) {
        FareRule fareRule = fareRuleRepository
                .findByRouteIdAndFromStopIdAndToStopId(routeId, fromStopId, toStopId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Fare rule not found for route %d from stop %d to stop %d",
                                routeId, fromStopId, toStopId)
                ));

        BigDecimal basePrice = fareRule.getBasePrice();
        log.info("Base price for route {}: {}", routeId, basePrice);

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Passenger with ID %d not found", passengerId)
                ));

        BigDecimal ageDiscount = calculateAgeDiscount(passenger.getBirthDate(), fareRule);
        log.info("Age discount for passenger {}: {}", passengerId, ageDiscount);

        Seat seat = seatRepository.findByBusIdAndNumber(busId, seatNumber)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat %s not found in bus %d", seatNumber, busId)
                ));

        BigDecimal seatSurcharge = calculateSeatSurcharge(seat, basePrice);
        log.info("Seat surcharge for {} seat: {}", seat.getType(), seatSurcharge);

        BigDecimal priceAfterDiscount = basePrice.multiply(
                BigDecimal.ONE.subtract(ageDiscount)
        );

        BigDecimal finalPrice = priceAfterDiscount.add(seatSurcharge);

        log.info("Final price calculated: base={}, afterDiscount={}, withSurcharge={}",
                basePrice, priceAfterDiscount, finalPrice);

        return finalPrice;
    }

    private BigDecimal calculateAgeDiscount(LocalDate birthDate, FareRule fareRule) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();

        try {
            Map<String, Double> discounts = objectMapper.readValue(
                    fareRule.getDiscounts(),
                    new TypeReference<Map<String, Double>>() {}
            );

            Double discountValue = null;

            if (age < 12) {
                discountValue = discounts.get("child");
            } else if (age >= 60) {
                discountValue = discounts.get("senior");
            } else if (age >= 12 && age < 26) {
                discountValue = discounts.get("student");
            }

            if (discountValue != null) {
                return BigDecimal.valueOf(discountValue);
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("Error parsing discounts JSON: {}", fareRule.getDiscounts(), e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateSeatSurcharge(Seat seat, BigDecimal basePrice) {
        if (seat.getType() == Seat.Type.PREFERENTIAL) {
            return basePrice.multiply(new BigDecimal("0.15"));
        }
        return BigDecimal.ZERO;
    }

    private void validateStopsBelongToRoute(Long routeId, Long fromStopId, Long toStopId) {
        Stop fromStop = stopRepository.findById(fromStopId)
                .orElseThrow(() -> new NotFoundException("From stop not found"));

        Stop toStop = stopRepository.findById(toStopId)
                .orElseThrow(() -> new NotFoundException("To stop not found"));

        if (!fromStop.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException(
                    String.format("From stop %d does not belong to route %d", fromStopId, routeId)
            );
        }

        if (!toStop.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException(
                    String.format("To stop %d does not belong to route %d", toStopId, routeId)
            );
        }
    }

    private void validateStopOrder(Stop fromStop, Stop toStop) {
        if (fromStop.getOrder() >= toStop.getOrder()) {
            throw new IllegalArgumentException(
                    String.format("From stop order (%d) must be less than to stop order (%d)",
                            fromStop.getOrder(), toStop.getOrder())
            );
        }
    }
}
