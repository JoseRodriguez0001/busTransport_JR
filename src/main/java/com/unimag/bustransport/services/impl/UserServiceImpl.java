package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.InvalidCredentialsException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.UserService;
import com.unimag.bustransport.services.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // TODO: Inyectar cuando se implemente Spring Security
    // private final PasswordEncoder passwordEncoder;

    @Override
    public UserDtos.UserResponse registerUser(UserDtos.UserCreateRequest request) {
        log.debug("Registering new user with email: {}", request.email());

        validateEmail(request.email());

        if (userRepository.existsByEmail((request.email()))) {
            log.error("Email already exists: {}", request.email());
            throw new DuplicateResourceException(
                    String.format("User with email %s already exists", request.email())
            );
        }

        if (request.phone() != null) {
            validatePhone(request.phone());
            if (userRepository.findByPhone((request.phone())).isPresent()) {
                log.error("Phone already exists: {}", request.phone());
                throw new DuplicateResourceException(
                        String.format("User with phone %s already exists", request.phone())
                );
            }
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPhone(request.phone());

        //  Forzar rol PASSENGER en registro público
        user.setRole(Role.ROLE_PASSENGER);

        // TODO: Encriptar password cuando se implemente Spring Security
        // user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordHash(request.password());

        user.setStatus(User.Status.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);

        log.info("Passenger registered successfully with ID: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    // ⭐ NUEVO: Crear empleados (CLERK, DRIVER, DISPATCHER) y ADMIN
    // TODO: Agregar @PreAuthorize("hasRole('ADMIN')") cuando implementes Spring Security
    @Override
    public UserDtos.UserResponse createEmployee(UserDtos.EmployeeCreateRequest request) {
        log.debug("Creating employee with email: {} and role: {}", request.email(), request.role());

        validateEmail(request.email());

        // Validar que el rol NO sea PASSENGER
        if (request.role() == Role.ROLE_PASSENGER) {
            log.error("Cannot create PASSENGER accounts with createEmployee method");
            throw new IllegalArgumentException(
                    "Cannot create PASSENGER accounts with this method. Use public registration endpoint."
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            log.error("Email already exists: {}", request.email());
            throw new DuplicateResourceException(
                    String.format("User with email %s already exists", request.email())
            );
        }

        if (request.phone() != null) {
            validatePhone(request.phone());
            if (userRepository.findByPhone(request.phone()).isPresent()) {
                log.error("Phone already exists: {}", request.phone());
                throw new DuplicateResourceException(
                        String.format("User with phone %s already exists", request.phone())
                );
            }
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setRole(request.role());  //  Ahora sí acepta el rol (ADMIN, CLERK, DRIVER, DISPATCHER)

        // Generar contraseña temporal
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(tempPassword);

        user.setStatus(User.Status.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);

        log.info("Employee created with ID: {} and role: {}. Temporary password: {}",
                savedUser.getId(), savedUser.getRole(), tempPassword);

        // TODO: Enviar email con contraseña temporal cuando implementes email service
        // emailService.sendTemporaryPassword(savedUser.getEmail(), tempPassword);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDtos.UserResponse login(String email, String password) {
        log.debug("Login attempt for email: {}", email);

        User user = userRepository.findByEmail((email)).orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (user.getStatus() != User.Status.ACTIVE) {
            log.error("User account is not active. Status: {}", user.getStatus());
            throw new InvalidCredentialsException(
                    String.format("User account is %s", user.getStatus())
            );
        }

        // TODO: Usar passwordEncoder.matches() cuando implementes Spring Security
        if (!user.getPasswordHash().equals(password)) {
            log.error("Invalid password for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User logged in successfully: {} with role: {}", email, user.getRole());

        // TODO: Retornar JWT token cuando implementes Spring Security
        return userMapper.toResponse(user);
    }

    @Override
    public void updateUser(Long id, UserDtos.UserUpdateRequest request) {
        log.debug("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("User with ID %d not found", id)
                    );
                });

        // Validar y actualizar teléfono
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            validatePhone(request.phone());
            userRepository.findByPhone(request.phone()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new DuplicateResourceException(
                            String.format("Phone %s is already in use", request.phone())
                    );
                }
            });
        }

        // Solo actualizar campos permitidos
        // NO actualizar: role, email, status, password

        if (request.name() != null) {
            user.setName(request.name());
            log.debug("Updated fullName to: {}", request.name());
        }

        if (request.phone() != null) {
            user.setPhone(request.phone());
            log.debug("Updated phone to: {}", request.phone());
        }

        userRepository.save(user);

        log.info("User updated successfully with ID: {}", id);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        log.debug("Changing password for user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("User with ID %d not found", id)
                    );
                });

        // Validar contraseña actual
        // TODO: Usar passwordEncoder.matches() cuando implementes Spring Security
        if (!user.getPasswordHash().equals(oldPassword)) {
            log.error("Invalid old password for user ID: {}", id);
            throw new InvalidCredentialsException("Invalid old password");
        }

        // Validar que la nueva sea diferente
        if (oldPassword.equals(newPassword)) {
            log.error("New password is the same as old password for user ID: {}", id);
            throw new IllegalArgumentException("New password must be different from old password");
        }

        // Validar fortaleza de contraseña
        validatePasswordStrength(newPassword);

        // TODO: Encriptar cuando implementes Spring Security
        user.setPasswordHash(newPassword);
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", id);
    }

    @Override
    public void desactivateUser(Long id) {
        log.debug("Deactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("User with ID %d not found", id)
                    );
                });

        if (user.getStatus() == User.Status.INACTIVE) {
            log.warn("User with ID {} is already inactive", id);
            throw new IllegalStateException(
                    String.format("User with ID %d is already inactive", id)
            );
        }

        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);

        log.info("User deactivated successfully with ID: {}", id);
    }

    @Override
    public void reactivateUser(Long id) {
        log.debug("Reactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("User with ID %d not found", id)
                    );
                });

        if (user.getStatus() == User.Status.ACTIVE) {
            log.warn("User with ID {} is already active", id);
            throw new IllegalStateException(
                    String.format("User with ID %d is already active", id)
            );
        }

        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);

        log.info("User reactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDtos.UserResponse getUserById(Long id) {
        log.debug("Getting user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new NotFoundException(
                            String.format("User with ID %d not found", id)
                    );
                });

        return userMapper.toResponse(user);
    }

    //  No exponer como endpoint público
    // Solo para uso interno del servicio o por ADMIN
    @Override
    @Transactional(readOnly = true)
    public UserDtos.UserResponse getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new NotFoundException(
                            String.format("User with email %s not found", email)
                    );
                });

        return userMapper.toResponse(user);
    }

    // ⚠️ MÉTODO INTERNO: No exponer como endpoint público
    // Solo para uso interno del servicio o por ADMIN
    @Override
    @Transactional(readOnly = true)
    public UserDtos.UserResponse getUserByPhone(String phone) {
        log.debug("Getting user by phone: {}", phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    log.error("User not found with phone: {}", phone);
                    return new NotFoundException(
                            String.format("User with phone %s not found", phone)
                    );
                });

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> getAllUsersByRole(Role role) {
        log.debug("Getting all users by role: {}", role);

        List<User> users = userRepository.findActiveUsersByRole((role));

        log.info("Found {} users with role: {}", users.size(), role);

        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return;  // Phone es opcional
        }

        // Formato colombiano: 10 dígitos, empieza con 3
        String phoneRegex = "^3[0-9]{9}$";
        if (!phone.matches(phoneRegex)) {
            throw new IllegalArgumentException(
                    "Invalid phone format. Must be 10 digits starting with 3"
            );
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Opcional: Validar que tenga al menos una mayúscula, minúscula y número
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
    }

    private String generateTemporaryPassword() {
        // Genera contraseña temporal de 12 caracteres: Temp + 8 caracteres aleatorios
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder("Temp");

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}
