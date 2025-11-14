package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.api.dto.SeatDtos.SeatCreateRequest;
import com.unimag.bustransport.api.dto.SeatDtos.SeatResponse;
import com.unimag.bustransport.api.dto.SeatDtos.SeatUpdateRequest;
import com.unimag.bustransport.domain.entities.Seat;
import com.unimag.bustransport.services.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Validated
public class SeatController {

    private final SeatService service;

    @PostMapping
    public ResponseEntity<SeatResponse> create(@Valid @RequestBody SeatCreateRequest req,
                                               UriComponentsBuilder uriBuilder) {
        var seatCreated = service.createSeat(req);
        var location = uriBuilder.path("/api/v1/seats/{id}")
                .buildAndExpand(seatCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(seatCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSeatById(id));
    }

    @GetMapping("/by-bus/{busId}")
    public ResponseEntity<List<SeatResponse>> getByBus(@PathVariable Long busId) {
        return ResponseEntity.ok(service.getSeatsByBusId(busId));
    }

    @GetMapping("/by-bus/{busId}/type/{type}")
    public ResponseEntity<List<SeatResponse>> getByBusAndType(@PathVariable Long busId,
                                                              @PathVariable Seat.Type type) {
        return ResponseEntity.ok(service.getSeatsByBusIdAndType(busId, type));
    }

    @GetMapping("/check-availability")
    public ResponseEntity<SeatDtos.AvailabilityResponse> checkAvailability(
            @RequestParam Long tripId,
            @RequestParam String seatNumber,
            @RequestParam Long fromStopId,
            @RequestParam Long toStopId) {
        var available = service.isSeatAvailable(tripId, seatNumber, fromStopId, toStopId);
        return ResponseEntity.ok(new SeatDtos.AvailabilityResponse(available));
    }

    @PostMapping("/confirm-reservation")
    public ResponseEntity<Void> confirmReservation(
            @RequestParam Long tripId,
            @RequestParam List<String> seatNumbers,
            @RequestParam Long purchaseId) {
        service.confirmSeatReservation(tripId, seatNumbers, purchaseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody SeatUpdateRequest req) {
        service.updateSeat(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }

}