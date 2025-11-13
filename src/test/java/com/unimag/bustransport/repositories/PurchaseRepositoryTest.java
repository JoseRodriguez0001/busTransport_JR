package com.unimag.bustransport.repositories;

import com.unimag.bustransport.domain.entities.Purchase;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.PurchaseRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PurchaseRepositoryTest extends AbstractRepositoryTI{

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        purchaseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User givenUser(String email) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .passwordHash("hash")
                .role(Role.ROLE_PASSENGER)
                .createdAt(OffsetDateTime.now())
                .status(User.Status.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Debe encontrar purchases por user ID")
    void shouldFindPurchasesByUserId() {
        // Given
        User user = givenUser("user@example.com");

        Purchase purchase1 = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod(Purchase.PaymentMethod.CASH)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .build();
        purchaseRepository.save(purchase1);

        Purchase purchase2 = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(75000))
                .paymentMethod(Purchase.PaymentMethod.CARD)
                .paymentStatus(Purchase.PaymentStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        purchaseRepository.save(purchase2);

        // When
        List<Purchase> found = purchaseRepository.findByUserId(user.getId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Purchase::getTotalAmount)
                .containsExactlyInAnyOrder(
                        BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(75000)
                );
    }

    @Test
    @DisplayName("Debe encontrar purchases en rango de fechas")
    void shouldFindPurchasesByDateRange() {
        // Given
        User user = givenUser("user@example.com");
        OffsetDateTime now = OffsetDateTime.now();

        Purchase purchase1 = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(30000))
                .paymentMethod(Purchase.PaymentMethod.CASH)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .createdAt(now.minusDays(2))
                .build();
        purchaseRepository.save(purchase1);

        Purchase purchase2 = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(40000))
                .paymentMethod(Purchase.PaymentMethod.TRANSFER)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .createdAt(now.minusDays(1))
                .build();
        purchaseRepository.save(purchase2);

        Purchase purchase3 = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(60000))
                .paymentMethod(Purchase.PaymentMethod.QR)
                .paymentStatus(Purchase.PaymentStatus.PENDING)
                .createdAt(now.minusDays(10))
                .build();
        purchaseRepository.save(purchase3);

        // When
        List<Purchase> found = purchaseRepository.findByDateRange(
                now.minusDays(3),
                now
        );

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Purchase::getTotalAmount)
                .containsExactlyInAnyOrder(
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(40000)
                );
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando user no tiene purchases")
    void shouldReturnEmptyListWhenUserHasNoPurchases() {
        // Given
        User user = givenUser("user@example.com");
        // No se crean purchases

        // When
        List<Purchase> found = purchaseRepository.findByUserId(user.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay purchases en el rango de fechas")
    void shouldReturnEmptyListWhenNoPurchasesInDateRange() {
        // Given
        User user = givenUser("user@example.com");
        OffsetDateTime now = OffsetDateTime.now();

        Purchase purchase = Purchase.builder()
                .user(user)
                .totalAmount(BigDecimal.valueOf(50000))
                .paymentMethod(Purchase.PaymentMethod.CASH)
                .paymentStatus(Purchase.PaymentStatus.CONFIRMED)
                .createdAt(now.minusDays(20))
                .build();
        purchaseRepository.save(purchase);

        // When - Buscar en un rango donde no hay purchases
        List<Purchase> found = purchaseRepository.findByDateRange(
                now.minusDays(5),
                now
        );

        // Then
        assertThat(found).isEmpty();
    }
}
