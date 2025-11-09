package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.Role;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserService {
    UserDtos.UserResponse registerUser(UserDtos.UserCreateRequest request);
    UserDtos.UserResponse createEmployee(UserDtos.EmployeeCreateRequest request);
    UserDtos.UserResponse login(String email, String password);
    void updateUser(Long id,UserDtos.UserUpdateRequest request);
    void changePassword(Long id, String oldPassword, String newPassword);
    void desactivateUser(@Param("id") Long id);
    void reactivateUser(Long id);
    UserDtos.UserResponse getUserById(@Param("id") Long id);
    UserDtos.UserResponse getUserByEmail(@Param("email") String email);
    UserDtos.UserResponse getUserByPhone(@Param("phone") String phone);
    List<UserDtos.UserResponse> getAllUsersByRole(Role role);
}
