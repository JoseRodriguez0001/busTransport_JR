package com.unimag.bustransport.config;

import com.unimag.bustransport.security.config.JwtProperties;
import com.unimag.bustransport.security.jwt.JwtAuthenticationFilter;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.shaded.com.trilead.ssh2.auth.AuthenticationManager;

/**
 * Configuración de seguridad para tests de controladores.
 * Deshabilita toda la seguridad para facilitar el testing de endpoints.
 *
 * Uso: Agregar @Import(TestSecurityConfig.class) en los tests de controladores con @WebMvcTest
 */
@TestConfiguration
public class TestSecurityConfig {


    // Mocks de beans de seguridad
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private JwtProperties jwtProperties;

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // Deshabilita seguridad para los tests
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}