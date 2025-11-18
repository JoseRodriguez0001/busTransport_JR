package com.unimag.bustransport.security.config;

import com.unimag.bustransport.security.jwt.JwtAuthenticationFilter;
import com.unimag.bustransport.security.jwt.JwtService;
import com.unimag.bustransport.security.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // ========== ENDPOINTS PÚBLICOS ==========
                        .requestMatchers(
                                "/api/v1/users/register",
                                "/api/v1/users/login",
                                "/error"
                        ).permitAll()

                        // Ver rutas y buscar viajes (público)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/routes/**",
                                "/api/v1/trips/search"
                        ).permitAll()

                        // Crear pasajero para compra como invitado
                        .requestMatchers(HttpMethod.POST, "/api/v1/passengers").permitAll()

                        // ========== ADMINISTRACIÓN DE USUARIOS ==========
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/employees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/users/*/deactivate",
                                "/api/v1/users/*/reactivate"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/by-email/**",
                                "/api/v1/users/by-phone/**",
                                "/api/v1/users/by-role/**"
                        ).hasAnyRole("CLERK", "ADMIN")

                        // Ver y editar perfil propio (se valida ownership en servicio si es necesario)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/users/*",
                                "/api/v1/users/*/change-password"
                        ).authenticated()

                        // ========== PASAJEROS ==========
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/passengers/**")
                        .hasAnyRole("CLERK", "ADMIN")
                        .requestMatchers("/api/v1/passengers/**")
                        .hasAnyRole("PASSENGER", "CLERK", "ADMIN")

                        // ========== COMPRAS Y TICKETS ==========
                        .requestMatchers(HttpMethod.POST, "/api/v1/purchases/**")
                        .hasAnyRole("PASSENGER", "CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/purchases/**")
                        .hasAnyRole("PASSENGER", "CLERK", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/validate-qr")
                        .hasAnyRole("DRIVER", "DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers("/api/v1/tickets/**")
                        .hasAnyRole("PASSENGER", "DRIVER", "DISPATCHER", "CLERK", "ADMIN")

                        // ========== VIAJES Y RUTAS ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/trips/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/trips/**")
                        .hasAnyRole("DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/trips/**")
                        .hasAnyRole("DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trips/**")
                        .hasAnyRole("CLERK", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/routes/**")
                        .hasAnyRole("CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/routes/**")
                        .hasAnyRole("CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/routes/**")
                        .hasAnyRole("CLERK", "ADMIN")

                        // ========== BUSES Y ASIENTOS ==========
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/buses/**",
                                "/api/v1/seats/**"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/buses/**",
                                "/api/v1/seats/**"
                        ).hasAnyRole("CLERK", "ADMIN")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/buses/**",
                                "/api/v1/seats/**"
                        ).hasAnyRole("CLERK", "ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/buses/**",
                                "/api/v1/seats/**"
                        ).hasAnyRole("CLERK", "ADMIN")

                        // ========== PARADAS ==========
                        .requestMatchers("/api/v1/stops/**")
                        .hasAnyRole("CLERK", "ADMIN")

                        // ========== RESERVAS DE ASIENTOS ==========
                        .requestMatchers("/api/v1/seat-holds/**")
                        .hasAnyRole("PASSENGER", "CLERK", "ADMIN")

                        // ========== EQUIPAJE ==========
                        .requestMatchers("/api/v1/baggage/**")
                        .hasAnyRole("PASSENGER", "CLERK", "ADMIN")

                        // ========== ENCOMIENDAS/PAQUETES ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/parcels/**")
                        .hasAnyRole("PASSENGER", "DRIVER", "DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/parcels/**")
                        .hasAnyRole("CLERK", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/parcels/*/assign-trip",
                                "/api/v1/parcels/*/confirm-delivery",
                                "/api/v1/parcels/*/mark-failed"
                        ).hasAnyRole("DRIVER", "DISPATCHER", "CLERK", "ADMIN")

                        // ========== ASIGNACIONES ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/assignments/by-driver/**")
                        .hasAnyRole("DRIVER", "DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers("/api/v1/assignments/**")
                        .hasAnyRole("DISPATCHER", "CLERK", "ADMIN")

                        // ========== REGLAS DE TARIFA ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/fare-rules/**")
                        .authenticated()
                        .requestMatchers("/api/v1/fare-rules/**")
                        .hasAnyRole("CLERK", "ADMIN")

                        // ========== INCIDENTES ==========
                        .requestMatchers(HttpMethod.POST, "/api/v1/incidents/**")
                        .hasAnyRole("DRIVER", "DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers("/api/v1/incidents/**")
                        .hasAnyRole("DISPATCHER", "CLERK", "ADMIN")

                        // ========== KPIs ==========
                        .requestMatchers(HttpMethod.GET, "/api/v1/kpis/**")
                        .hasAnyRole("DISPATCHER", "CLERK", "ADMIN")
                        .requestMatchers("/api/v1/kpis/**")
                        .hasRole("ADMIN")

                        // ========== CONFIGURACIONES ==========
                        .requestMatchers("/api/v1/configs/**")
                        .hasRole("ADMIN")

                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

}