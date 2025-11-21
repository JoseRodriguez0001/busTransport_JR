package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.security.dto.AuthDtos;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetails;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import com.unimag.bustransport.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(
                1L,
                "test@example.com",
                "Test User",
                "encodedPassword",
                "ROLE_PASSENGER",
                true
        );
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario exitosamente")
    void testRegisterUser_Success() throws Exception {
        // Given
        UserDtos.UserCreateRequest request = new UserDtos.UserCreateRequest(
                "test@example.com",
                "Password123",
                "Test User",
                "3001234567"
        );

        UserDtos.UserResponse response = new UserDtos.UserResponse(
                1L,
                "test@example.com",
                "Test User",
                "3001234567",
                Role.ROLE_PASSENGER,
                "ACTIVE",
                OffsetDateTime.now()
        );

        when(userService.registerUser(any(UserDtos.UserCreateRequest.class))).thenReturn(response);

        // When & Then
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_PASSENGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).registerUser(any(UserDtos.UserCreateRequest.class));
    }

    @Test
    @DisplayName("Debe fallar al registrar con email duplicado")
    void testRegisterUser_DuplicateEmail() throws Exception {
        // Given
        UserDtos.UserCreateRequest request = new UserDtos.UserCreateRequest(
                "duplicate@example.com",
                "Password123",
                "Test User",
                "3001234567"
        );

        when(userService.registerUser(any(UserDtos.UserCreateRequest.class)))
                .thenThrow(new com.unimag.bustransport.exception.DuplicateResourceException(
                        "User with email duplicate@example.com already exists"
                ));

        // When & Then
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with email duplicate@example.com already exists"));
    }

    @Test
    @DisplayName("Debe fallar al registrar con contraseña débil")
    void testRegisterUser_WeakPassword() throws Exception {
        // Given
        UserDtos.UserCreateRequest request = new UserDtos.UserCreateRequest(
                "test@example.com",
                "weak",
                "Test User",
                "3001234567"
        );

        when(userService.registerUser(any(UserDtos.UserCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("Password must be at least 8 characters long"));

        // When & Then
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long"));
    }

    @Test
    @DisplayName("Debe fallar al registrar con formato de email inválido")
    void testRegisterUser_InvalidEmailFormat() throws Exception {
        // Given
        UserDtos.UserCreateRequest request = new UserDtos.UserCreateRequest(
                "invalid-email",
                "Password123",
                "Test User",
                "3001234567"
        );

        when(userService.registerUser(any(UserDtos.UserCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        // When & Then
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe hacer login exitosamente y retornar tokens JWT")
    void testLogin_Success() throws Exception {
        // Given
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                "test@example.com",
                "Password123"
        );

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token-123");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token-456");
        when(jwtService.extractExpiration("access-token-123")).thenReturn(new Date(System.currentTimeMillis() + 3600000));

        // When & Then
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.role").value("ROLE_PASSENGER"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(userDetails);
        verify(jwtService, times(1)).generateRefreshToken(userDetails);
    }

    @Test
    @DisplayName("Debe fallar al hacer login con credenciales inválidas")
    void testLogin_InvalidCredentials() throws Exception {
        // Given
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                "test@example.com",
                "WrongPassword"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Debe fallar al hacer login con usuario inexistente")
    void testLogin_UserNotFound() throws Exception {
        // Given
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(
                "nonexistent@example.com",
                "Password123"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        // When & Then
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe renovar token exitosamente con refresh token válido")
    void testRefreshToken_Success() throws Exception {
        // Given
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest(
                "refresh-token-456"
        );

        when(jwtService.isRefreshTokenValid("refresh-token-456")).thenReturn(true);
        when(jwtService.extractUserId("refresh-token-456")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access-token-789");
        when(jwtService.extractExpiration("new-access-token-789")).thenReturn(new Date(System.currentTimeMillis() + 3600000));

        // When & Then
        mvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token-789"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());

        verify(jwtService, times(1)).isRefreshTokenValid("refresh-token-456");
        verify(jwtService, times(1)).extractUserId("refresh-token-456");
        verify(jwtService, times(1)).generateAccessToken(userDetails);
    }

    @Test
    @DisplayName("Debe fallar al renovar token con refresh token inválido")
    void testRefreshToken_InvalidToken() throws Exception {
        // Given
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest(
                "invalid-refresh-token"
        );

        when(jwtService.isRefreshTokenValid("invalid-refresh-token")).thenReturn(false);

        // When & Then
        mvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        verify(jwtService, times(1)).isRefreshTokenValid("invalid-refresh-token");
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Debe fallar al renovar token expirado")
    void testRefreshToken_ExpiredToken() throws Exception {
        // Given
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest(
                "expired-refresh-token"
        );

        when(jwtService.isRefreshTokenValid("expired-refresh-token")).thenReturn(false);

        // When & Then
        mvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("Debe fallar al registrar sin email")
    void testRegisterUser_MissingEmail() throws Exception {
        // Given - request sin email
        String invalidRequest = """
            {
                "password": "Password123",
                "name": "Test User",
                "phone": "3001234567"
            }
            """;

        // When & Then
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe fallar al hacer login sin password")
    void testLogin_MissingPassword() throws Exception {
        // Given - request sin password
        String invalidRequest = """
            {
                "email": "test@example.com"
            }
            """;

        // When & Then
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}