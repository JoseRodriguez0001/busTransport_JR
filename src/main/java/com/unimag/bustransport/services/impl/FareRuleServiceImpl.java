package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.*;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.FareRuleService;
import com.unimag.bustransport.services.mapper.FareRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    private final TicketRepository ticketRepository;
    private final BusRepository busRepository;
    private final FareRuleMapper fareRuleMapper;
    private final TripRepository tripRepository;
    private final ConfigService configService;

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
            fareRule.setDynamicPricing(request.dynamicPricing());
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
                                     Long passengerId, Long busId, String seatNumber, Long tripId) {

        // 1. Obtener FareRule
        FareRule fareRule = fareRuleRepository
                .findByRouteIdAndFromStopIdAndToStopId(routeId, fromStopId, toStopId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Fare rule not found for route %d from stop %d to stop %d",
                                routeId, fromStopId, toStopId)
                ));

        BigDecimal basePrice = fareRule.getBasePrice();
        log.info("Base price for route {}: {}", routeId, basePrice);

        // 2. Calcular descuento por edad
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Passenger with ID %d not found", passengerId)
                ));

        BigDecimal ageDiscount = calculateAgeDiscount(passenger.getBirthDate(), fareRule);
        log.info("Age discount for passenger {}: {}", passengerId, ageDiscount);

        // 3. Calcular recargo por asiento preferencial
        Seat seat = seatRepository.findByBusIdAndNumber(busId, seatNumber)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat %s not found in bus %d", seatNumber, busId)
                ));

        BigDecimal seatSurcharge = calculateSeatSurcharge(seat, basePrice);
        log.info("Seat surcharge for {} seat: {}", seat.getType(), seatSurcharge);

        // 4. Aplicar descuento
        BigDecimal priceAfterDiscount = basePrice.multiply(
                BigDecimal.ONE.subtract(ageDiscount)
        );

        // 5.  Calcular recargo dinámico (si está activado)
        BigDecimal dynamicSurcharge = BigDecimal.ZERO;
        if (fareRule.getDynamicPricing() == FareRule.DynamicPricing.ON) {
            dynamicSurcharge = calculateDynamicSurcharge(tripId, busId, basePrice);
            log.info("Dynamic pricing surcharge: {} (fare rule has dynamic pricing enabled)", dynamicSurcharge);
        }

        // 6. Precio final
        BigDecimal finalPrice = priceAfterDiscount
                .add(seatSurcharge)
                .add(dynamicSurcharge);

        log.info("Final price calculated: base={}, afterDiscount={}, withSurcharge={}, withDynamic={}",
                basePrice, priceAfterDiscount, priceAfterDiscount.add(seatSurcharge), finalPrice);

        return finalPrice;
    }

    // Calcular recargo dinámico basado en ocupación o tiempo
    private BigDecimal calculateDynamicSurcharge(Long tripId, Long busId, BigDecimal basePrice) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Bus with ID %d not found", busId)
                ));
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Trip with ID %d not found", tripId)
                ));

        // Contar tickets SOLD para este trip
        long soldSeats = ticketRepository.countSoldByTrip(tripId);
        double occupancyRate = (double) soldSeats / bus.getCapacity();

        log.debug("Trip {} occupancy: {} / {} = {}%",
                tripId, soldSeats, bus.getCapacity(), (int)(occupancyRate * 100));

        BigDecimal surcharge = BigDecimal.ZERO;

        // Recargo por ocupación
        if (occupancyRate >= 0.85) {
            // Ocupación >= 85% → +20%
            surcharge = basePrice.multiply(new BigDecimal("0.20"));
            log.info("High occupancy ({}%) surcharge applied: +20%", (int)(occupancyRate * 100));
        } else if (occupancyRate >= 0.70) {
            // Ocupación >= 70% → +10%
            surcharge = basePrice.multiply(new BigDecimal("0.10"));
            log.info("Medium occupancy ({}%) surcharge applied: +10%", (int)(occupancyRate * 100));
        } else {
            log.info("No dynamic surcharge applied (occupancy: {}%)", (int)(occupancyRate * 100));
        }

        // 3. Recargo por venta a menos de 5 minutos del inicio
        OffsetDateTime now = OffsetDateTime.now();
        long minutesToDeparture = Duration.between(now, trip.getDepartureAt()).toMinutes();

        if (minutesToDeparture <= 5 && minutesToDeparture >= 0) {
            BigDecimal lastMinuteSurcharge = basePrice.multiply(configService.getValueAsBigDecimal("no-show.policy"));
            surcharge = surcharge.add(lastMinuteSurcharge);

            log.info("Last-minute sale surcharge applied: +15% ({} minutes before departure)",
                    minutesToDeparture);
        }

        return surcharge;
    }

    private BigDecimal calculateAgeDiscount(LocalDate birthDate, FareRule fareRule) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        Map<String, Double> discounts = fareRule.getDiscounts();

        if (discounts == null || discounts.isEmpty()) {
            log.debug("No discounts configured for fare rule ID {}", fareRule.getId());
            return BigDecimal.ZERO;
        }

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
