package com.unimag.bustransport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimag.bustransport.api.dto.PassengerDtos.*;
import com.unimag.bustransport.config.TestSecurityConfig;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.PassengerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassengerController.class)
@Import(TestSecurityConfig.class)
class PassengerControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean PassengerService service;

    @Test
    void create_shouldReturn201AndLocation() throws Exception {
        var req = new PassengerCreateRequest("Robert Martínez García", "CC", "1234567890",
                LocalDate.of(1995, 5, 15), "+573001234567", 5L);
        var resp = new PassengerResponse(1L, "Robert Martínez García", "CC", "1234567890",
                LocalDate.of(1995, 5, 15), "+573001234567");

        when(service.createPassenger(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/passengers/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Robert Martínez García"))
                .andExpect(jsonPath("$.documentNumber").value("1234567890"));
    }

    @Test
    void get_shouldReturn200() throws Exception {
        when(service.getPassengerById(3L))
                .thenReturn(new PassengerResponse(3L, "Jose García", "CC", "9876543210",
                        LocalDate.of(1992, 8, 20), "+573201234567"));

        mvc.perform(get("/api/v1/passengers/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.fullName").value("Jose García"));
    }

    @Test
    void get_shouldReturn404WhenNotFound() throws Exception {
        when(service.getPassengerById(999L)).thenThrow(new NotFoundException("Passenger with ID 999 not found"));

        mvc.perform(get("/api/v1/passengers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Passenger with ID 999 not found"));
    }

    @Test
    void getByDocumentNumber_shouldReturn200() throws Exception {
        when(service.finByDocumentNumber("1234567890"))
                .thenReturn(new PassengerResponse(1L, "Robert Martínez García", "CC", "1234567890",
                        LocalDate.of(1995, 5, 15), "+573001234567"));

        mvc.perform(get("/api/v1/passengers/by-document/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("1234567890"));
    }

    @Test
    void getByUser_shouldReturn200() throws Exception {
        var passengers = List.of(
                new PassengerResponse(1L, "Robert Martínez", "CC", "1234567890",
                        LocalDate.of(1995, 5, 15), "+573001234567"),
                new PassengerResponse(2L, "Claudia López", "CC", "1122334455",
                        LocalDate.of(1998, 3, 10), "+573109876543")
        );

        when(service.getPassengerByUser(5L)).thenReturn(passengers);

        mvc.perform(get("/api/v1/passengers/by-user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturn204() throws Exception {
        var req = new PassengerUpdateRequest("Robert Martínez Updated", "CC", "1234567890",
                LocalDate.of(1995, 5, 15), "+573009999999");

        mvc.perform(patch("/api/v1/passengers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(service).updatePassenger(eq(1L), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mvc.perform(delete("/api/v1/passengers/1"))
                .andExpect(status().isNoContent());

        verify(service).deletePassenger(1L);
    }
}
