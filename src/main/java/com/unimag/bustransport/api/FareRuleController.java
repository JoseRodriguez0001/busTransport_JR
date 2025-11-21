package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.FareRuleDtos;
import com.unimag.bustransport.services.FareRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fare-rules")
@RequiredArgsConstructor
@Validated
public class FareRuleController {

    private final FareRuleService fareRuleService;

    @PostMapping
    public ResponseEntity<FareRuleDtos.FareRuleResponse> create(
            @Valid @RequestBody FareRuleDtos.FareRuleCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        var fareRuleCreated = fareRuleService.createFareRule(request);
        var location = uriBuilder.path("/api/v1/fare-rules/{id}")
                .buildAndExpand(fareRuleCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(fareRuleCreated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody FareRuleDtos.FareRuleUpdateRequest request) {
        fareRuleService.updateFareRule(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fareRuleService.deleteFareRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareRuleDtos.FareRuleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(fareRuleService.getFareRule(id));
    }

    @GetMapping
    public ResponseEntity<List<FareRuleDtos.FareRuleResponse>> getAll() {
        return ResponseEntity.ok(fareRuleService.getAllFareRules());
    }

    @GetMapping("/by-route/{routeId}")
    public ResponseEntity<List<FareRuleDtos.FareRuleResponse>> getByRouteId(
            @PathVariable Long routeId) {
        return ResponseEntity.ok(fareRuleService.getFareRulesByRouteId(routeId));
    }

    @GetMapping("/calculate-price")
    public ResponseEntity<FareRuleDtos.PriceResponse> calculatePrice(
            @RequestParam Long routeId,
            @RequestParam Long fromStopId,
            @RequestParam Long toStopId,
            @RequestParam Long passengerId,
            @RequestParam Long busId,
            @RequestParam String seatNumber,
            @RequestParam Long tripId) {
        var price = fareRuleService.calculatePrice(
                routeId, fromStopId, toStopId, passengerId, busId, seatNumber, tripId
        );
        return ResponseEntity.ok(new FareRuleDtos.PriceResponse(price));
    }
}