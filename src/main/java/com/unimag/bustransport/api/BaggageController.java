package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.BaggageDtos.BaggageCreateRequest;
import com.unimag.bustransport.api.dto.BaggageDtos.BaggageResponse;
import com.unimag.bustransport.api.dto.BaggageDtos.BaggageUpdateRequest;
import com.unimag.bustransport.services.BaggageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/baggage")
@RequiredArgsConstructor
@Validated
public class BaggageController {

    private final BaggageService service;

    @PostMapping
    public ResponseEntity<BaggageResponse> create(@Valid @RequestBody BaggageCreateRequest req,
                                                  UriComponentsBuilder uriBuilder) {
        var baggageCreated = service.registerBaggage(req);
        var location = uriBuilder.path("/api/v1/baggage/{id}")
                .buildAndExpand(baggageCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(baggageCreated);
    }

    @GetMapping("/by-ticket/{ticketId}")
    public ResponseEntity<List<BaggageResponse>> getByTicketId(@PathVariable Long ticketId) {
        return ResponseEntity.ok(service.getBaggageByTicket(ticketId));
    }

    @GetMapping("/calculate-fee")
    public ResponseEntity<BigDecimal> calculateFee(@RequestParam Double weightKg) {
        return ResponseEntity.ok(service.calculateBaggageFee(weightKg));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody BaggageUpdateRequest req) {
        service.updateBaggage(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBaggage(id);
        return ResponseEntity.noContent().build();
    }
}