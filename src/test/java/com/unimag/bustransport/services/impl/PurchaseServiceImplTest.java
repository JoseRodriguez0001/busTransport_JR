package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.PurchaseDtos;
import com.unimag.bustransport.api.dto.TicketDtos;
import com.unimag.bustransport.domain.entities.*;
import com.unimag.bustransport.domain.repositories.PurchaseRepository;
import com.unimag.bustransport.domain.repositories.SeatHoldRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.FareRuleService;
import com.unimag.bustransport.services.SeatHoldService;
import com.unimag.bustransport.services.TicketService;
import com.unimag.bustransport.services.mapper.PurchaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseService Unit Tests")
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private SeatHoldService seatHoldService;

    @Mock
    private FareRuleService fareRuleService;

    @Mock
    private TicketService ticketService;

    private final PurchaseMapper purchaseMapper = Mappers.getMapper(PurchaseMapper.class);

    private PurchaseServiceImpl purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseServiceImpl(
                purchaseRepository,
                userRepository,
                tripRepository,
                purchaseMapper,
                seatHoldRepository,
                seatHoldService,
                fareRuleService,
                ticketService
        );
    }

    private User givenUser() {
        return User.builder()
                .id(1L)
                .name("testuser")
                .email("test@example.com")
                .role(Role.ROLE_PASSENGER)
                .status(User.Status.ACTIVE)
                .passwordHash("hash")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Trip givenTrip() {
        Route route = Route.builder()
                .id(1L)
                .code("R001")
                .build();

        Bus bus = Bus.builder()
                .id(1L)
                .plate("ABC123")
                .capacity(40)
                .build();

        return Trip.builder()
                .id(1L)
                .route(route)
                .bus(bus)
                .date(LocalDate.now())
                .status(Trip.Status.SCHEDULED)
                .build();
    }

    private Purchase givenPurchase(User user, Purchase.PaymentStatus status) {
        Purchase purchase = Purchase.builder()
                .id(1L)
                .user(user)
                .totalAmount(BigDecimal.valueOf(100000))
                .paymentMethod(Purchase.PaymentMethod.CARD)
                .paymentStatus(status)
                .createdAt(OffsetDateTime.now())
                .tickets(new ArrayList<>())
                .build();

        // Agregar un ticket de ejemplo
        Ticket ticket = Ticket.builder()
                .id(1L)
                .seatNumber("A1")
                .price(BigDecimal.valueOf(50000))
                .status(Ticket.Status.PENDING)
                .purchase(purchase)
                .trip(givenTrip())
                .build();

        purchase.getTickets().add(ticket);
        return purchase;
    }

    private PurchaseDtos.PurchaseCreateRequest givenCreateRequest() {
        PurchaseDtos.PurchaseCreateRequest.TicketRequest ticketRequest =
                new PurchaseDtos.PurchaseCreateRequest.TicketRequest(
                        1L,  // tripId
                        1L,  // passengerId
                        "A1", // seatNumber
                        1L,  // fromStopId
                        2L,  // toStopId
                        null // baggage
                );

        return new PurchaseDtos.PurchaseCreateRequest(
                1L,  // userId
                "CARD",
                List.of(ticketRequest)
        );
    }

    @Test
    @DisplayName("Debe crear purchase correctamente")
    void shouldCreatePurchase() {
        // Given
        User user = givenUser();
        Trip trip = givenTrip();
        PurchaseDtos.PurchaseCreateRequest request = givenCreateRequest();
        Purchase savedPurchase = givenPurchase(user, Purchase.PaymentStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        doNothing().when(seatHoldService).validateActiveHolds(anyLong(), anyList(), anyLong());
        when(fareRuleService.calculatePrice(anyLong(), anyLong(), anyLong(), anyLong(),
                anyLong(), anyString(), anyLong()))
                .thenReturn(BigDecimal.valueOf(50000));
        when(ticketService.createTicket(any(TicketDtos.TicketCreateRequest.class)))
                .thenReturn(null);
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(savedPurchase);

        // When
        PurchaseDtos.PurchaseResponse response = purchaseService.createPurchase(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo("PENDING");

        verify(userRepository, times(1)).findById(1L);
        verify(tripRepository, times(1)).findById(1L);
        verify(seatHoldService, times(1)).validateActiveHolds(anyLong(), anyList(), anyLong());
        verify(fareRuleService, times(1)).calculatePrice(anyLong(), anyLong(), anyLong(),
                anyLong(), anyLong(), anyString(), anyLong());
        verify(ticketService, times(1)).createTicket(any(TicketDtos.TicketCreateRequest.class));
        verify(purchaseRepository, times(2)).save(any(Purchase.class)); // 2 veces: inicial + actualizar total
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando user no existe")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        PurchaseDtos.PurchaseCreateRequest request = givenCreateRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID 1 not found");

        verify(userRepository, times(1)).findById(1L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando tickets pertenecen a diferentes trips")
    void shouldThrowExceptionWhenTicketsDifferentTrips() {
        // Given
        User user = givenUser();

        PurchaseDtos.PurchaseCreateRequest.TicketRequest ticket1 =
                new PurchaseDtos.PurchaseCreateRequest.TicketRequest(
                        1L, 1L, "A1", 1L, 2L, null
                );

        PurchaseDtos.PurchaseCreateRequest.TicketRequest ticket2 =
                new PurchaseDtos.PurchaseCreateRequest.TicketRequest(
                        2L, 1L, "A2", 1L, 2L, null // Diferente tripId
                );

        PurchaseDtos.PurchaseCreateRequest request = new PurchaseDtos.PurchaseCreateRequest(
                1L, "CARD", List.of(ticket1, ticket2)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All tickets in a purchase must belong to the same trip");

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Debe obtener purchase por ID")
    void shouldGetPurchaseById() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.CONFIRMED);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        // When
        PurchaseDtos.PurchaseResponse response = purchaseService.getPurchase(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(purchaseRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando purchase no existe")
    void shouldThrowExceptionWhenPurchaseNotFound() {
        // Given
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> purchaseService.getPurchase(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Purchase with ID 999 not found");

        verify(purchaseRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Debe obtener purchases por user ID")
    void shouldGetPurchasesByUserId() {
        // Given
        User user = givenUser();
        List<Purchase> purchases = List.of(givenPurchase(user, Purchase.PaymentStatus.CONFIRMED));

        when(userRepository.existsById(1L)).thenReturn(true);
        when(purchaseRepository.findByUserId(1L)).thenReturn(purchases);

        // When
        List<PurchaseDtos.PurchaseResponse> result = purchaseService.getPurchasesByUserId(1L);

        // Then
        assertThat(result).hasSize(1);

        verify(userRepository, times(1)).existsById(1L);
        verify(purchaseRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Debe confirmar purchase correctamente")
    void shouldConfirmPurchase() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.PENDING);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        doNothing().when(seatHoldService).validateActiveHolds(anyLong(), anyList(), anyLong());
        doNothing().when(ticketService).generateQrForTicket(anyLong());
        when(seatHoldRepository.findByTripIdAndSeatNumberAndUserId(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

        // When
        purchaseService.confirmPurchase(1L, "PAY-REF-123");

        // Then
        verify(purchaseRepository, times(1)).findById(1L);
        verify(seatHoldService, times(1)).validateActiveHolds(anyLong(), anyList(), anyLong());
        verify(ticketService, times(1)).generateQrForTicket(anyLong());
        verify(purchaseRepository, times(1)).save(purchase);
    }

    @Test
    @DisplayName("Debe lanzar excepción al confirmar purchase que no está PENDING")
    void shouldThrowExceptionWhenConfirmingNonPendingPurchase() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.CONFIRMED);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        // When & Then
        assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, "PAY-REF-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot confirm purchase with ID 1");

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando SeatHolds han expirado al confirmar")
    void shouldThrowExceptionWhenSeatHoldsExpiredOnConfirm() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.PENDING);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        doThrow(new RuntimeException("Holds expired"))
                .when(seatHoldService).validateActiveHolds(anyLong(), anyList(), anyLong());

        // When & Then
        assertThatThrownBy(() -> purchaseService.confirmPurchase(1L, "PAY-REF-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("seat holds have expired");

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Debe cancelar purchase correctamente")
    void shouldCancelPurchase() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.PENDING);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(seatHoldRepository.findByTripIdAndSeatNumberAndUserId(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);

        // When
        purchaseService.cancelPurchase(1L);

        // Then
        verify(purchaseRepository, times(1)).findById(1L);
        verify(purchaseRepository, times(1)).save(purchase);
    }

    @Test
    @DisplayName("Debe lanzar excepción al cancelar purchase CONFIRMED")
    void shouldThrowExceptionWhenCancellingConfirmedPurchase() {
        // Given
        User user = givenUser();
        Purchase purchase = givenPurchase(user, Purchase.PaymentStatus.CONFIRMED);

        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        // When & Then
        assertThatThrownBy(() -> purchaseService.cancelPurchase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel purchase with ID 1. Payment already confirmed");

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("Debe obtener purchases por rango de fechas")
    void shouldGetPurchasesByDateRange() {
        // Given
        User user = givenUser();
        OffsetDateTime start = OffsetDateTime.now().minusDays(7);
        OffsetDateTime end = OffsetDateTime.now();
        List<Purchase> purchases = List.of(givenPurchase(user, Purchase.PaymentStatus.CONFIRMED));

        when(purchaseRepository.findByDateRange(start, end)).thenReturn(purchases);

        // When
        List<PurchaseDtos.PurchaseResponse> result =
                purchaseService.getPurchasesByDateRange(start, end);

        // Then
        assertThat(result).hasSize(1);

        verify(purchaseRepository, times(1)).findByDateRange(start, end);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando fecha inicio es después de fecha fin")
    void shouldThrowExceptionWhenStartDateAfterEndDate() {
        // Given
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = OffsetDateTime.now().minusDays(7);

        // When & Then
        assertThatThrownBy(() -> purchaseService.getPurchasesByDateRange(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date must be before end date");

        verify(purchaseRepository, never()).findByDateRange(any(), any());
    }
}