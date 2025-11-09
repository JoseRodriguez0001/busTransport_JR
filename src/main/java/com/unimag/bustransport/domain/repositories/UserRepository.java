package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    UserDtos.UserResponse registerUser(UserDtos.UserCreateRequest request);

    UserDtos.UserResponse createEmployee(UserDtos.EmployeeCreateRequest request);

    UserDtos.UserResponse login(String email, String password);

    void updateUser(Long id, UserDtos.UserUpdateRequest request);

    void changePassword(Long id, String oldPassword, String newPassword);

    void desactivateUser(Long id);

    void reactivateUser(Long id);

    UserDtos.UserResponse getUserById(Long id);


    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    List<User> findActiveUsersByRole(Role role);
}
