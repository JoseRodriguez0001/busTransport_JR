package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.AssignmentDtos.AssignmentCreateRequest;
import com.unimag.bustransport.api.dto.AssignmentDtos.AssignmentResponse;
import com.unimag.bustransport.api.dto.AssignmentDtos.AssignmentUpdateRequest;
import com.unimag.bustransport.services.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Validated
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody AssignmentCreateRequest req,
                                                     UriComponentsBuilder uriBuilder) {
        var assignmentCreated = service.createAssignment(req);
        var location = uriBuilder.path("/api/v1/assignments/{id}")
                .buildAndExpand(assignmentCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(assignmentCreated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAssignment(id));
    }

    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<AssignmentResponse> getByTripId(@PathVariable Long tripId) {
        return ResponseEntity.ok(service.getAssignmentByTripId(tripId));
    }

    @GetMapping("/by-driver/{driverId}")
    public ResponseEntity<List<AssignmentResponse>> getByDriverId(@PathVariable Long driverId) {
        return ResponseEntity.ok(service.getAssignmentByDriverId(driverId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody AssignmentUpdateRequest req) {
        service.updateAssignment(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}