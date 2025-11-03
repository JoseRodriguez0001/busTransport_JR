package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.services.PassengerService;

import java.util.List;

public class PassengerServiceImpl implements PassengerService {
    @Override
    public PassengerDtos.PassengerResponse createPassenger(PassengerDtos.PassengerCreateRequest request) {
        return null;
    }

    @Override
    public void updatePassenger(Long id, PassengerDtos.PassengerUpdateRequest request) {

    }

    @Override
    public void deletePassenger(Long id) {

    }

    @Override
    public List<PassengerDtos.PassengerResponse> getPassengerByUser(Long userId) {
        return List.of();
    }

    @Override
    public PassengerDtos.PassengerResponse finByDocumentNumber(String documentNumber) {
        return null;
    }

    @Override
    public PassengerDtos.PassengerResponse getPassengerById(Long id) {
        return null;
    }
}
