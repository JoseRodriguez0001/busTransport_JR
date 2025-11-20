package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.api.dto.TicketDtos.TicketResponse;
import com.unimag.bustransport.security.user.CustomUserDetails;
import com.unimag.bustransport.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService service;

    //UN TICKET SOLO SE CREA AL CREAR UN PURCHASE, NO EXPONEMOS UN CONTROLADOR PARA CREAR UN TICKET POR APARTE


    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTicket(id));
    }

    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<List<TicketResponse>> getByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(service.getTicketsByTrip(tripId));
    }

    @GetMapping("/by-purchase/{purchaseId}")
    public ResponseEntity<List<TicketResponse>> getByPurchase(@PathVariable Long purchaseId) {
        return ResponseEntity.ok(service.getTicketsByPurchase(purchaseId));
    }

    @GetMapping("/by-passenger/{passengerId}")
    public ResponseEntity<List<TicketResponse>> getByPassenger(@PathVariable Long passengerId) {
        return ResponseEntity.ok(service.getTicketsByPassenger(passengerId));
    }

    @PostMapping("/{id}/generate-qr")
    public ResponseEntity<Void> generateQr(@PathVariable Long id) {
        service.generateQrForTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-qr")
    public ResponseEntity<TicketDtos.ValidationResponse> validateQr(@RequestParam String qrCode) {
        try {
            service.validateQrForTicket(qrCode);
            return ResponseEntity.ok(new TicketDtos.ValidationResponse(true, "Valid ticket"));
        } catch (Exception e) {
            return ResponseEntity.ok(new TicketDtos.ValidationResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{id}/refund-ticket")
    public ResponseEntity<TicketDtos.ValidationResponse> refundTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        service.refundTicket(id, user.getUserId());
        return  ResponseEntity.ok(new TicketDtos.ValidationResponse(true, "Ticket refunded"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}