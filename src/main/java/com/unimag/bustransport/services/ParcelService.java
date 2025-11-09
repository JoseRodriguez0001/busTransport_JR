package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.ParcelDtos;

import java.util.List;

public interface ParcelService {
    ParcelDtos.ParcelResponse createParcel(ParcelDtos.ParcelCreateRequest request);

    //solo actualiza datos como phone, stops, etc. NO trips
    void updateParcel(Long parcelId, ParcelDtos.ParcelUpdateRequest request);

    void assignTrip(Long parcelId, Long tripId);

    List<ParcelDtos.ParcelResponse> getParcelsBySender(String senderPhone);

    List<ParcelDtos.ParcelResponse> getParcelsByReceiver(String receiverPhone);

    List<ParcelDtos.ParcelResponse> getParcelsByTrip(Long tripId);

    ParcelDtos.ParcelResponse getParcelByCode(String code);

    void confirmDelivery(Long parcelId, String otp, String proofPhotoUrl);

    void markAsFailed(Long parcelId, String failureReason);
}
