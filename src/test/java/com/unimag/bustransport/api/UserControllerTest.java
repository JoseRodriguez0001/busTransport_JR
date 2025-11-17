package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.UserDtos.*;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean UserService service;


    @Test
    void createEmployee_shouldReturn201() throws Exception {
        var req = new EmployeeCreateRequest("julian.driver@bustransport.com", "Julian Rojas", "+573109876543", Role.ROLE_DRIVER);
        var resp = userResponse(10L, "julian.driver@bustransport.com", "Julian Rojas", "+573109876543", Role.ROLE_DRIVER);

        when(service.createEmployee(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/users/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_DRIVER"))
                .andExpect(jsonPath("$.email").value("julian.driver@bustransport.com"));
    }


    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getUserById(3L))
                .thenReturn(userResponse(3L, "claudia@gmail.com", "Claudia López", "+573151234567", Role.ROLE_PASSENGER));

        mvc.perform(get("/api/v1/users/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Claudia López"));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getUserById(999L)).thenThrow(new NotFoundException("User with ID 999 not found"));

        mvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with ID 999 not found"));
    }

    @Test
    void getByEmail_shouldReturn200() throws Exception {
        when(service.getUserByEmail("robert@gmail.com"))
                .thenReturn(userResponse(1L, "robert@gmail.com", "Robert Martínez", "+573001234567", Role.ROLE_PASSENGER));

        mvc.perform(get("/api/v1/users/by-email/robert@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("robert@gmail.com"));
    }

    @Test
    void getByRole_shouldReturn200() throws Exception {
        var drivers = List.of(
                userResponse(10L, "julian@bustransport.com", "Julian Rojas", "+573109876543", Role.ROLE_DRIVER),
                userResponse(11L, "carlos@bustransport.com", "Carlos Pérez", "+573201112233", Role.ROLE_DRIVER)
        );

        when(service.getAllUsersByRole(Role.ROLE_DRIVER)).thenReturn(drivers);

        mvc.perform(get("/api/v1/users/by-role/ROLE_DRIVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("ROLE_DRIVER"));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new UserUpdateRequest("Robert Martínez Updated", "+573009999999");

        mvc.perform(patch("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updateUser(eq(1L), any());
    }

    @Test
    void changePassword_shouldReturn204() throws Exception {
        mvc.perform(patch("/api/v1/users/1/change-password")
                        .param("oldPassword", "old123")
                        .param("newPassword", "new456"))
                .andExpect(status().isNoContent());

        verify(service).changePassword(1L, "old123", "new456");
    }

    @Test
    void deactivate_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/users/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(service).desactivateUser(1L);
    }

    @Test
    void reactivate_shouldReturn204() throws Exception {
        mvc.perform(post("/api/v1/users/1/reactivate"))
                .andExpect(status().isNoContent());

        verify(service).reactivateUser(1L);
    }

    // Helper
    private UserResponse userResponse(Long id, String email, String name, String phone, Role role) {
        return new UserResponse(id, email, name, phone, role, "ACTIVE", OffsetDateTime.parse("2025-11-14T10:00:00-05:00"));
    }
}