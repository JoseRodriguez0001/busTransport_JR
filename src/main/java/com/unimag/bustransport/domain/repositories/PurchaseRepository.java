package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase,Long> {
    List<Purchase> findByUserId(Long userId);
    List<Purchase> findByPaymentStatus(Purchase.PaymentStatus status);

    @Query("SELECT p FROM Purchase p WHERE p.createdAt BETWEEN :start AND :end")
    List<Purchase> findByDateRange(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
