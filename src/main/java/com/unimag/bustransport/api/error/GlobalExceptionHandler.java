package com.unimag.bustransport.api.error;

import com.unimag.bustransport.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.InvalidCredentialsException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, WebRequest request) {
        var body= ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false), List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        var violations = ex.getBindingResult().getFieldErrors()
                .stream().map(fe-> new ApiError.FieldViolation(fe.getField(),fe.getDefaultMessage())).toList();
        var body= ApiError.of(HttpStatus.BAD_REQUEST, "Validation failed", request.getDescription(false), violations);
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, WebRequest request) {
        var violations = ex.getConstraintViolations().stream()
                .map(v-> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage())).toList();
        var body= ApiError.of(HttpStatus.BAD_REQUEST, "Constraint violation", request.getDescription(false), violations);
        return ResponseEntity.badRequest().body(body);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex, WebRequest req) {
        var body = ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getDescription(false), List.of());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, WebRequest req) {
        var body = ApiError.of(HttpStatus.CONFLICT, ex.getMessage(), req.getDescription(false), List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest req) {
        var body = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getDescription(false), List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

   //security
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex,
            WebRequest request
    ) {
        var body = ApiError.of(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed: " + ex.getMessage(),
                request.getDescription(false),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

     @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
            WebRequest request
    ) {
        var body = ApiError.of(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                request.getDescription(false),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request
    ) {
        var body = ApiError.of(
                HttpStatus.FORBIDDEN,
                "Access denied: You don't have permission to access this resource",
                request.getDescription(false),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

     @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
            DuplicateResourceException ex,
            WebRequest request
    ) {
        var body = ApiError.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getDescription(false),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex,
            WebRequest request
    ) {
        var body = ApiError.of(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getDescription(false),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

// ========================================
// CÓDIGOS DE STATUS HTTP -
// ========================================

    /*
     * 200 OK: Operación exitosa
     * 201 Created: Recurso creado exitosamente
     * 204 No Content: Operación exitosa sin body
     *
     * 400 Bad Request: Request inválido (validación fallida)
     * 401 Unauthorized: No autenticado (sin token o token inválido)
     * 403 Forbidden: Autenticado pero sin permiso (rol incorrecto)
     * 404 Not Found: Recurso no existe
     * 409 Conflict: Recurso duplicado
     *
     * 500 Internal Server Error: Error del servidor
     */

}
