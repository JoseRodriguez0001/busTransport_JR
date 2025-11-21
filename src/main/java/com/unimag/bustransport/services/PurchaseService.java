package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.PurchaseDtos;

import java.time.OffsetDateTime;
import java.util.List;

public interface PurchaseService {
    PurchaseDtos.PurchaseResponse createPurchase(PurchaseDtos.PurchaseCreateRequest request);

    PurchaseDtos.PurchaseResponse getPurchase(Long purchaseId);

    List<PurchaseDtos.PurchaseResponse> getPurchasesByUserId(Long userId);

    void confirmPurchase(Long purchaseId, String PaymentReference);

    void cancelPurchase(Long purchaseId);

    List<PurchaseDtos.PurchaseResponse> getPurchasesByDateRange(OffsetDateTime start, OffsetDateTime end);
}