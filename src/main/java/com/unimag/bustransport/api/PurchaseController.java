package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.PurchaseDtos.PurchaseCreateRequest;
import com.unimag.bustransport.api.dto.PurchaseDtos.PurchaseResponse;
import com.unimag.bustransport.services.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@Validated
public class PurchaseController {

    private final PurchaseService service;

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseCreateRequest req,
                                                   UriComponentsBuilder uriBuilder) {
        var purchaseCreated = service.createPurchase(req);
        var location = uriBuilder.path("/api/v1/purchases/{id}")
                .buildAndExpand(purchaseCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(purchaseCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPurchase(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<PurchaseResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getPurchasesByUserId(userId));
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<PurchaseResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return ResponseEntity.ok(service.getPurchasesByDateRange(start, end));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long id,
                                        @RequestParam String paymentReference) {
        service.confirmPurchase(id, paymentReference);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancelPurchase(id);
        return ResponseEntity.noContent().build();
    }
}