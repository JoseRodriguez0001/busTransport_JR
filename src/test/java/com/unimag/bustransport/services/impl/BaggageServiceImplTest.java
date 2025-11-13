package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BaggageDtos;
import com.unimag.bustransport.domain.entities.Baggage;
import com.unimag.bustransport.domain.entities.Ticket;
import com.unimag.bustransport.domain.repositories.BaggageRepository;
import com.unimag.bustransport.domain.repositories.TicketRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.BaggageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaggageService Unit Tests")
class BaggageServiceImplTest {

    @Mock
    private BaggageRepository baggageRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ConfigService configService;

    private final BaggageMapper baggageMapper = Mappers.getMapper(BaggageMapper.class);

    private BaggageServiceImpl baggageService;

    @BeforeEach
    void setUp() {
        baggageService = new BaggageServiceImpl(
                baggageRepository,
                ticketRepository,
                baggageMapper,
                configService
        );
    }

    private Ticket givenSoldTicket() {
        return Ticket.builder()
                .id(1L)
                .seatNumber("A1")
                .price(BigDecimal.valueOf(50000))
                .status(Ticket.Status.SOLD)
                .qrCode("QR-A1")
                .build();
    }

    private Ticket givenPendingTicket() {
        return Ticket.builder()
                .id(2L)
                .seatNumber("B2")
                .status(Ticket.Status.PENDING)
                .build();
    }

    private Baggage givenBaggage(Ticket ticket) {
        return Baggage.builder()
                .id(1L)
                .ticket(ticket)
                .weightKg(25.0)
                .fee(BigDecimal.valueOf(15000))
                .tagCode("BAG-00000001-12345")
                .build();
    }

    private BaggageDtos.BaggageCreateRequest givenCreateRequest() {
        return new BaggageDtos.BaggageCreateRequest(
                1L,   // ticketId
                25.0  // weightKg
        );
    }

    private BaggageDtos.BaggageUpdateRequest givenUpdateRequest() {
        return new BaggageDtos.BaggageUpdateRequest(
                30.0,           // weightKg
                "NEW-TAG-CODE"  // tagCode
        );
    }


    @Test
    @DisplayName("Debe registrar equipaje correctamente")
    void shouldRegisterBaggage() {
        // Given
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = givenCreateRequest();
        Baggage savedBaggage = givenBaggage(ticket);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(savedBaggage);

        // When
        BaggageDtos.BaggageResponse response = baggageService.registerBaggage(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.weightKg()).isEqualTo(25.0);

        verify(ticketRepository, times(1)).findById(1L);
        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG");
        verify(baggageRepository, times(1)).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no existe")
    void shouldThrowExceptionWhenTicketNotFound() {
        // Given
        BaggageDtos.BaggageCreateRequest request = givenCreateRequest();

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket no encontrado");

        verify(ticketRepository, times(1)).findById(1L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no está SOLD")
    void shouldThrowExceptionWhenTicketNotSold() {
        // Given
        Ticket pendingTicket = givenPendingTicket();
        BaggageDtos.BaggageCreateRequest request = new BaggageDtos.BaggageCreateRequest(
                2L,   // ticketId del pending
                25.0
        );

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(pendingTicket));

        // When & Then
        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot register baggage for ticket with status PENDING");

        verify(ticketRepository, times(1)).findById(2L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }


    @Test
    @DisplayName("Debe actualizar equipaje correctamente")
    void shouldUpdateBaggage() {
        // Given
        Ticket ticket = givenSoldTicket();
        Baggage existingBaggage = givenBaggage(ticket);
        BaggageDtos.BaggageUpdateRequest request = givenUpdateRequest();

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(existingBaggage));
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(existingBaggage);

        // When
        baggageService.updateBaggage(1L, request);

        // Then
        verify(baggageRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).save(existingBaggage);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar equipaje inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentBaggage() {
        // Given
        BaggageDtos.BaggageUpdateRequest request = givenUpdateRequest();

        when(baggageRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> baggageService.updateBaggage(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Equipaje no encontrado");

        verify(baggageRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe actualizar solo los campos no nulos")
    void shouldUpdateOnlyNonNullFields() {
        // Given
        Ticket ticket = givenSoldTicket();
        Baggage existingBaggage = givenBaggage(ticket);
        BaggageDtos.BaggageUpdateRequest partialRequest = new BaggageDtos.BaggageUpdateRequest(
                null,           // weightKg sin cambiar
                "NEW-TAG-ONLY"  // solo cambiar tagCode
        );

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(existingBaggage));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(existingBaggage);

        // When
        baggageService.updateBaggage(1L, partialRequest);

        // Then
        verify(baggageRepository, times(1)).save(existingBaggage);
        verify(configService, never()).getValueAsBigDecimal(anyString());
    }


    @Test
    @DisplayName("Debe obtener equipajes por ticket ID")
    void shouldGetBaggageByTicket() {
        // Given
        Ticket ticket = givenSoldTicket();
        List<Baggage> baggages = List.of(
                givenBaggage(ticket),
                givenBaggage(ticket)
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(baggageRepository.findByTicketId(1L)).thenReturn(baggages);

        // When
        List<BaggageDtos.BaggageResponse> result = baggageService.getBaggageByTicket(1L);

        // Then
        assertThat(result).hasSize(2);

        verify(ticketRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).findByTicketId(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no existe al buscar equipajes")
    void shouldThrowExceptionWhenTicketNotFoundInGetBaggage() {
        // Given
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> baggageService.getBaggageByTicket(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket no encontrado");

        verify(ticketRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).findByTicketId(anyLong());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando ticket no tiene equipajes")
    void shouldReturnEmptyListWhenTicketHasNoBaggage() {
        // Given
        Ticket ticket = givenSoldTicket();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(baggageRepository.findByTicketId(1L)).thenReturn(List.of());

        // When
        List<BaggageDtos.BaggageResponse> result = baggageService.getBaggageByTicket(1L);

        // Then
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Debe calcular tarifa cero cuando peso está dentro del límite gratuito")
    void shouldCalculateZeroFeeWhenWeightWithinFreeLimit() {
        // Given
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));

        // When
        BigDecimal fee = baggageService.calculateBaggageFee(15.0);

        // Then
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);

        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
    }

    @Test
    @DisplayName("Debe calcular tarifa correctamente cuando hay exceso de peso")
    void shouldCalculateFeeCorrectlyWhenWeightExceedsLimit() {
        // Given
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));

        // When
        BigDecimal fee = baggageService.calculateBaggageFee(25.0); // 5kg de exceso

        // Then
        // 5kg exceso * 3000 = 15000
        assertThat(fee).isEqualByComparingTo(BigDecimal.valueOf(15000));

        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG");
    }

    @Test
    @DisplayName("Debe calcular tarifa cero cuando peso es exactamente el límite")
    void shouldCalculateZeroFeeWhenWeightEqualsLimit() {
        // Given
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));

        // When
        BigDecimal fee = baggageService.calculateBaggageFee(20.0);

        // Then
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }


    @Test
    @DisplayName("Debe eliminar equipaje correctamente")
    void shouldDeleteBaggage() {
        // Given
        Ticket ticket = givenSoldTicket();
        Baggage baggage = givenBaggage(ticket);

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(baggage));
        doNothing().when(baggageRepository).delete(baggage);

        // When
        baggageService.deleteBaggage(1L);

        // Then
        verify(baggageRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).delete(baggage);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar equipaje inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentBaggage() {
        // Given
        when(baggageRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> baggageService.deleteBaggage(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Equipaje no encontrado");

        verify(baggageRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).delete(any(Baggage.class));
    }
}