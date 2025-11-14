package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.security.dto.AuthDtos;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetails;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    
    @PostMapping("/register")
    public ResponseEntity<UserDtos.UserResponse> register(
            @Valid @RequestBody UserDtos.UserCreateRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        log.info("Nuevo registro de usuario: {}", request.email());
        
        UserDtos.UserResponse userCreated = userService.registerUser(request);
        
        var location = uriBuilder.path("/api/users/{id}")
                .buildAndExpand(userCreated.id())
                .toUri();
        
        return ResponseEntity.created(location).body(userCreated);
    }
    
     @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request
    ) {
        log.info("Intento de login: {}", request.email());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        
         CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
       String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        AuthDtos.AuthResponse response = new AuthDtos.AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.extractExpiration(accessToken).getTime() / 1000,  // Convertir a segundos
                new AuthDtos.AuthResponse.UserInfo(
                        userDetails.getUserId(),
                        userDetails.getEmail(),
                        userDetails.getName(),
                        userDetails.getRole()
                )
        );
        
        log.info("Login exitoso: {} con rol {}", request.email(), userDetails.getRole());
        
        return ResponseEntity.ok(response);
    }
    
   @PostMapping("/refresh-token")
    public ResponseEntity<AuthDtos.RefreshTokenResponse> refreshToken(
            @Valid @RequestBody AuthDtos.RefreshTokenRequest request
    ) {
        log.info("Solicitud de renovación de token");
        
         if (!jwtService.isRefreshTokenValid(request.refreshToken())) {
            log.error("Refresh token inválido o expirado");
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        
       Long userId = jwtService.extractUserId(request.refreshToken());
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(userId);
        
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        
        AuthDtos.RefreshTokenResponse response = new AuthDtos.RefreshTokenResponse(
                newAccessToken,
                "Bearer",
                jwtService.extractExpiration(newAccessToken).getTime() / 1000
        );
        
        log.info("Token renovado exitosamente para usuario: {}", userDetails.getEmail());
        
        return ResponseEntity.ok(response);
    }
}
