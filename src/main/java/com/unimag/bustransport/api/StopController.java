package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.StopDtos.StopCreateRequest;
import com.unimag.bustransport.api.dto.StopDtos.StopResponse;
import com.unimag.bustransport.api.dto.StopDtos.StopUpdateRequest;
import com.unimag.bustransport.services.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stops")
@RequiredArgsConstructor
@Validated
public class StopController {

    private final StopService service;

    @PostMapping
    public ResponseEntity<StopResponse> create(@Valid @RequestBody StopCreateRequest req,
                                               UriComponentsBuilder uriBuilder) {
        var stopCreated = service.createStop(req);
        var location = uriBuilder.path("/api/v1/stops/{id}")
                .buildAndExpand(stopCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(stopCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StopResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStopById(id));
    }

    @GetMapping("/by-route/{routeId}")
    public ResponseEntity<List<StopResponse>> getByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(service.getStopsByRouteId(routeId));
    }

    @GetMapping("/by-route/{routeId}/between")
    public ResponseEntity<List<StopResponse>> getStopsBetween(
            @PathVariable Long routeId,
            @RequestParam Integer fromOrder,
            @RequestParam Integer toOrder) {
        return ResponseEntity.ok(service.getStopsBetween(routeId, fromOrder, toOrder));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody StopUpdateRequest req) {
        service.updateStop(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteStop(id);
        return ResponseEntity.noContent().build();
    }
}