package com.unimag.bustransport.security.jwt;

import com.unimag.bustransport.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. Extraer el header Authorization
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 2. Extraer el token (quitar el prefijo "Bearer ")
            final String jwt = authHeader.substring(7);  // "Bearer ".length() = 7
            
            // 3. Extraer el userId del token
            final Long userId = jwtService.extractUserId(jwt);
            
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 5. Cargar el usuario de la base de datos
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                     UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                   authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                     SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Usuario autenticado: {} con rol: {}", 
                            userDetails.getUsername(), 
                            userDetails.getAuthorities());
                }
            }
            
        } catch (Exception e) {
             log.error("Error al procesar token JWT: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
