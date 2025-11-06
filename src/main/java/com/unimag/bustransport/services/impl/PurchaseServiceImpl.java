package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.PurchaseDtos;
import com.unimag.bustransport.api.dto.ReceiptDto;
import com.unimag.bustransport.domain.entities.Purchase;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.PurchaseRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.PurchaseService;
import com.unimag.bustransport.services.mapper.PurchaseMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final PurchaseMapper purchaseMapper;

    @Override
    public PurchaseDtos.PurchaseResponse createPurchase(PurchaseDtos.PurchaseCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Purchase purchase = purchaseMapper.toEntity(request);
        purchase.setUser(user);
        purchase.setPaymentStatus(Purchase.PaymentStatus.PENDING);
        purchase.setCreatedAt(OffsetDateTime.now());

        purchaseRepository.save(purchase);

        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public PurchaseDtos.PurchaseResponse getPurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Compra no encontrada"));

        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public List<PurchaseDtos.PurchaseResponse> getPurchasesByUserId(Long userId) {
        return List.of();
    }

    @Override
    public void confirmPurchase(Long purchaseId) {

    }

    @Override
    public void cancelPurchase(Long purchaseId) {

    }

    @Override
    public ReceiptDto generateReceipt(Long purchaseId) {
        return null;
    }
}
