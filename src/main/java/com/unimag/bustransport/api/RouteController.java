package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.RouteDtos.RouteCreateRequest;
import com.unimag.bustransport.api.dto.RouteDtos.RouteResponse;
import com.unimag.bustransport.api.dto.RouteDtos.RouteUpdateRequest;
import com.unimag.bustransport.api.dto.StopDtos.StopResponse;
import com.unimag.bustransport.services.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Validated
public class RouteController {

    private final RouteService service;

    @PostMapping
    public ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteCreateRequest req,
                                                UriComponentsBuilder uriBuilder) {
        var routeCreated = service.createRoute(req);
        var location = uriBuilder.path("/api/v1/routes/{id}")
                .buildAndExpand(routeCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(routeCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRouteById(id));
    }

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAll() {
        return ResponseEntity.ok(service.getAllRoutes());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RouteResponse>> search(
            @RequestParam String origin,
            @RequestParam String destination) {
        return ResponseEntity.ok(service.searchRoutes(origin, destination));
    }

    @GetMapping("/{id}/stops")
    public ResponseEntity<List<StopResponse>> getStops(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStopsByRouteId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody RouteUpdateRequest req) {
        service.updateRoute(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}