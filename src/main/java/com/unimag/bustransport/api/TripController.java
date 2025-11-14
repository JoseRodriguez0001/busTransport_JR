package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.SeatDtos.SeatResponse;
import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.api.dto.TripDtos.TripCreateRequest;
import com.unimag.bustransport.api.dto.TripDtos.TripResponse;
import com.unimag.bustransport.api.dto.TripDtos.TripUpdateRequest;
import com.unimag.bustransport.services.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Validated
public class TripController {

    private final TripService service;

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripCreateRequest req,
                                               UriComponentsBuilder uriBuilder) {
        var tripCreated = service.createTrip(req);
        var location = uriBuilder.path("/api/v1/trips/{id}")
                .buildAndExpand(tripCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(tripCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTripDetails(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripResponse>> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.getTrips(origin, destination, date));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSeats(id));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<TripDtos.StatisticsResponse> getStatistics(@PathVariable Long id) {
        var soldSeats = service.getTripStatistics(id);
        return ResponseEntity.ok(new TripDtos.StatisticsResponse(soldSeats));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody TripUpdateRequest req) {
        service.updateTrip(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

}