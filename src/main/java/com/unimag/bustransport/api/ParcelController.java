package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.ParcelDtos.ParcelCreateRequest;
import com.unimag.bustransport.api.dto.ParcelDtos.ParcelResponse;
import com.unimag.bustransport.api.dto.ParcelDtos.ParcelUpdateRequest;
import com.unimag.bustransport.services.ParcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
@Validated
public class ParcelController {

    private final ParcelService service;

    @PostMapping
    public ResponseEntity<ParcelResponse> create(@Valid @RequestBody ParcelCreateRequest req,
                                                 UriComponentsBuilder uriBuilder) {
        var parcelCreated = service.createParcel(req);
        var location = uriBuilder.path("/api/v1/parcels/{id}")
                .buildAndExpand(parcelCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(parcelCreated);
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<ParcelResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getParcelByCode(code));
    }

    @GetMapping("/by-sender")
    public ResponseEntity<List<ParcelResponse>> getBySender(@RequestParam String phone) {
        return ResponseEntity.ok(service.getParcelsBySender(phone));
    }

    @GetMapping("/by-receiver")
    public ResponseEntity<List<ParcelResponse>> getByReceiver(@RequestParam String phone) {
        return ResponseEntity.ok(service.getParcelsByReceiver(phone));
    }

    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<List<ParcelResponse>> getByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(service.getParcelsByTrip(tripId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody ParcelUpdateRequest req) {
        service.updateParcel(id, req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-trip")
    public ResponseEntity<Void> assignTrip(@PathVariable Long id,
                                           @RequestParam Long tripId) {
        service.assignTrip(id, tripId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<Void> confirmDelivery(@PathVariable Long id,
                                                @RequestParam String otp,
                                                @RequestParam(required = false) String proofPhotoUrl) {
        service.confirmDelivery(id, otp, proofPhotoUrl);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-failed")
    public ResponseEntity<Void> markFailed(@PathVariable Long id,
                                           @RequestParam String failureReason) {
        service.markAsFailed(id, failureReason);
        return ResponseEntity.noContent().build();
    }
}