package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.SeatHoldDtos;
import com.unimag.bustransport.api.dto.SeatHoldDtos.SeatHoldCreateRequest;
import com.unimag.bustransport.api.dto.SeatHoldDtos.SeatHoldResponse;
import com.unimag.bustransport.services.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seat-holds")
@RequiredArgsConstructor
@Validated
public class SeatHoldController {

    private final SeatHoldService service;

    @PostMapping
    public ResponseEntity<SeatHoldResponse> create(@Valid @RequestBody SeatHoldCreateRequest req,
                                                   UriComponentsBuilder uriBuilder) {
        var holdCreated = service.createSeatHold(req);
        var location = uriBuilder.path("/api/v1/seat-holds/{id}")
                .buildAndExpand(holdCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(holdCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatHoldResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getHoldById(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<SeatHoldResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getHoldsByUser(userId));
    }

    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<List<SeatHoldResponse>> getByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(service.getActiveHoldsByTrip(tripId));
    }

    @GetMapping("/by-trip/{tripId}/user/{userId}")
    public ResponseEntity<List<SeatHoldResponse>> getByTripAndUser(@PathVariable Long tripId,
                                                                   @PathVariable Long userId) {
        return ResponseEntity.ok(service.getActiveHoldsByTripAndUser(tripId, userId));
    }

    @GetMapping("/check-hold")
    public ResponseEntity<SeatHoldDtos.HoldCheckResponse> checkHold(
            @RequestParam Long tripId,
            @RequestParam String seatNumber) {
        var isOnHold = service.isSeatOnHold(tripId, seatNumber);
        return ResponseEntity.ok(new SeatHoldDtos.HoldCheckResponse(isOnHold));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        service.releaseSeatHold(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cleanup/expired")
    public ResponseEntity<SeatHoldDtos.CleanupResponse> markExpired() {
        var count = service.markExpiredHolds();
        return ResponseEntity.ok(new SeatHoldDtos.CleanupResponse(count, "Marked as expired"));
    }

    @DeleteMapping("/cleanup/expired")
    public ResponseEntity<SeatHoldDtos.CleanupResponse> deleteExpired() {
        var count = service.deleteExpiredHolds();
        return ResponseEntity.ok(new SeatHoldDtos.CleanupResponse(count, "Deleted"));
    }
}