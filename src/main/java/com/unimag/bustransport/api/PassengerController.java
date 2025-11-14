package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.PassengerDtos.PassengerCreateRequest;
import com.unimag.bustransport.api.dto.PassengerDtos.PassengerResponse;
import com.unimag.bustransport.api.dto.PassengerDtos.PassengerUpdateRequest;
import com.unimag.bustransport.security.user.CustomUserDetails;
import com.unimag.bustransport.services.PassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
@Validated
public class PassengerController {

    private final PassengerService service;

    @PostMapping
    public ResponseEntity<PassengerResponse> create(@Valid @RequestBody PassengerCreateRequest req,
                                                    UriComponentsBuilder uriBuilder) {
        var passengerCreated = service.createPassenger(req);
        var location = uriBuilder.path("/api/v1/passengers/{id}")
                .buildAndExpand(passengerCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(passengerCreated);
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPassengerById(id));
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-document/{documentNumber}")
    public ResponseEntity<PassengerResponse> getByDocumentNumber(@PathVariable String documentNumber) {
        return ResponseEntity.ok(service.finByDocumentNumber(documentNumber));
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-passengers")
    public ResponseEntity<List<PassengerResponse>> getMyPassengers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {  // ← AGREGAR ESTO
        return ResponseEntity.ok(service.getPassengerByUser(userDetails.getUserId()));
    }
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody PassengerUpdateRequest req) {
        service.updatePassenger(id, req);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePassenger(id);
        return ResponseEntity.noContent().build();
    }
}