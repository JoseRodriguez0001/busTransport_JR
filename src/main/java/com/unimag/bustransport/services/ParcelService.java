package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.ParcelDtos;
import com.unimag.bustransport.services.mapper.ParcelMapper;

import java.util.List;

public interface ParcelService {
    ParcelDtos.ParcelResponse createParcel(ParcelDtos.ParcelCreateRequest request);
    void updateParcel(ParcelDtos.ParcelUpdateRequest request);
    List<ParcelDtos.ParcelResponse> getParcelsBySender(String senderPhone);
    ParcelDtos.ParcelResponse getParcelByCode(String code);
    void confirmDelivery(Long parcelId, String otp);
}
