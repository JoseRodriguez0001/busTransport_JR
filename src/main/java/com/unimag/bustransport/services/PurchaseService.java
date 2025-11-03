package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.PurchaseDtos;
import com.unimag.bustransport.api.dto.ReceiptDto;

import java.util.List;

public interface PurchaseService {
    PurchaseDtos.PurchaseResponse createPurchase(PurchaseDtos.PurchaseCreateRequest request);
    PurchaseDtos.PurchaseResponse getPurchase(Long purchaseId);
    List<PurchaseDtos.PurchaseResponse> getPurchasesByUserId(Long userId);
    void confirmPurchase(Long purchaseId);
    void cancelPurchase(Long purchaseId);
    ReceiptDto generateReceipt(Long purchaseId);
}
