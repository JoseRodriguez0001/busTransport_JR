package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.BusDtos;
import com.unimag.bustransport.api.dto.SeatDtos;
import com.unimag.bustransport.services.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buses")
@RequiredArgsConstructor
@Validated
public class BusController {

    private final BusService busService;

    @PostMapping
    public ResponseEntity<BusDtos.BusResponse> create(
            @Valid @RequestBody BusDtos.BusCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        var busCreated = busService.createBus(request);
        var location = uriBuilder.path("/api/v1/buses/{id}")
                .buildAndExpand(busCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(busCreated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody BusDtos.BusUpdateRequest request) {
        busService.updateBus(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        busService.deleteBus(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusDtos.BusResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getBus(id));
    }

    @GetMapping
    public ResponseEntity<List<BusDtos.BusResponse>> getAll() {
        return ResponseEntity.ok(busService.getAllBus());
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatDtos.SeatResponse>> getAllSeats(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getAllSeatsByBusId(id));
    }
}