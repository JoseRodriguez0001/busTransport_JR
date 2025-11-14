package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.UserDtos.EmployeeCreateRequest;
import com.unimag.bustransport.api.dto.UserDtos.UserCreateRequest;
import com.unimag.bustransport.api.dto.UserDtos.UserResponse;
import com.unimag.bustransport.api.dto.UserDtos.UserUpdateRequest;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.security.user.CustomUserDetails;
import com.unimag.bustransport.services.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserCreateRequest req,
                                                 UriComponentsBuilder uriBuilder) {
        var userCreated = service.registerUser(req);
        var location = uriBuilder.path("/api/v1/users/{id}")
                .buildAndExpand(userCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(userCreated);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employees")
    public ResponseEntity<UserResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest req,
                                                       UriComponentsBuilder uriBuilder) {
        var employeeCreated = service.createEmployee(req);
        var location = uriBuilder.path("/api/v1/users/{id}")
                .buildAndExpand(employeeCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(employeeCreated);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(service.getUserById(userDetails.getUserId()));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.getUserByEmail(email));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-phone/{phone}")
    public ResponseEntity<UserResponse> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(service.getUserByPhone(phone));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable Role role) {
        return ResponseEntity.ok(service.getAllUsersByRole(role));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")  // ← Sin {id}
    public ResponseEntity<Void> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest req) {
        service.updateUser(userDetails.getUserId(), req);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/change-password")  // ← Sin {id}
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        service.changePassword(userDetails.getUserId(), oldPassword, newPassword);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.desactivateUser(id);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        service.reactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}