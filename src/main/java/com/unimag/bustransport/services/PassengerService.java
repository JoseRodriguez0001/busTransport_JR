package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PassengerService {
    PassengerDtos.PassengerResponse createPassenger(PassengerDtos.PassengerCreateRequest request);
    void updatePassenger(Long id,PassengerDtos.PassengerUpdateRequest request);
    void deletePassenger(Long id);
    List<PassengerDtos.PassengerResponse> getPassengerByUser(Long userId);
    PassengerDtos.PassengerResponse finByDocumentNumber(@Param("documentNumber") String documentNumber);
    PassengerDtos.PassengerResponse getPassengerById(@Param("id") Long id);
}
