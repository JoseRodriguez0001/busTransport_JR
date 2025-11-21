package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.UserDtos.EmployeeCreateRequest;
import com.unimag.bustransport.api.dto.UserDtos.UserResponse;
import com.unimag.bustransport.api.dto.UserDtos.UserUpdateRequest;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;


    @PostMapping("/employees")
    public ResponseEntity<UserResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest req,
                                                       UriComponentsBuilder uriBuilder) {
        var employeeCreated = service.createEmployee(req);
        var location = uriBuilder.path("/api/v1/users/{id}")
                .buildAndExpand(employeeCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(employeeCreated);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.getUserByEmail(email));
    }

    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<UserResponse> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(service.getUserByPhone(phone));
    }

    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable Role role) {
        return ResponseEntity.ok(service.getAllUsersByRole(role));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody UserUpdateRequest req) {
        service.updateUser(id, req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @RequestParam String oldPassword,
                                               @RequestParam String newPassword) {
        service.changePassword(id, oldPassword, newPassword);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.desactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        service.reactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}