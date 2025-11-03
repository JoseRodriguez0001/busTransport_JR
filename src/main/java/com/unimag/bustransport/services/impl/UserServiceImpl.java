package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.UserDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service@RequiredArgsConstructor@Transactional
public class UserServiceImpl implements UserService {
    @Override
    public UserDtos.UserResponse registerUser(UserDtos.UserCreateRequest request) {

     return null;
    }

    @Override
    public UserDtos.UserResponse login(String email, String password) {
        return null;
    }

    @Override
    public void updateUser(Long id, UserDtos.UserUpdateRequest request) {

    }

    @Override
    public void desactivateUser(Long id) {

    }

    @Override
    public UserDtos.UserResponse getUserById(Long id) {
        return null;
    }

    @Override
    public UserDtos.UserResponse getUserByEmail(String email) {
        return null;
    }

    @Override
    public UserDtos.UserResponse getUserByPhone(String phone) {
        return null;
    }

    @Override
    public List<UserDtos.UserResponse> getAllUsersByRole(Role role) {
        return List.of();
    }
}
