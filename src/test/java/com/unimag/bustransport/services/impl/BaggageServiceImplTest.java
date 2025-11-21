package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.BaggageDtos;
import com.unimag.bustransport.domain.entities.Baggage;
import com.unimag.bustransport.domain.entities.Ticket;
import com.unimag.bustransport.domain.repositories.BaggageRepository;
import com.unimag.bustransport.domain.repositories.TicketRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.BaggageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
class BaggageServiceImplTest {

    @Mock
    private BaggageRepository baggageRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ConfigService configService;

    @Spy
    private final BaggageMapper baggageMapper = Mappers.getMapper(BaggageMapper.class);

    @InjectMocks
    private BaggageServiceImpl baggageService;

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
                1L,
                25.0
        );
    }

    private BaggageDtos.BaggageUpdateRequest givenUpdateRequest() {
        return new BaggageDtos.BaggageUpdateRequest(
                30.0,
                "NEW-TAG-CODE"
        );
    }

    @Test
    @DisplayName("Debe registrar equipaje correctamente")
    void shouldRegisterBaggage() {
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = givenCreateRequest();
        Baggage savedBaggage = givenBaggage(ticket);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG"))
                .thenReturn(BigDecimal.valueOf(30.0));
        when(baggageRepository.countByTicketId(1L)).thenReturn(0);
        when(configService.getValueAsInt("MAX_BAGGAGE_PER_TICKET")).thenReturn(2);
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(savedBaggage);

        BaggageDtos.BaggageResponse response = baggageService.registerBaggage(request);

        assertThat(response).isNotNull();
        assertThat(response.weightKg()).isEqualTo(25.0);

        verify(ticketRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).countByTicketId(1L);
        verify(baggageRepository, times(1)).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no existe")
    void shouldThrowExceptionWhenTicketNotFound() {
        BaggageDtos.BaggageCreateRequest request = givenCreateRequest();

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket with ID 1 not found");

        verify(ticketRepository, times(1)).findById(1L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no está SOLD")
    void shouldThrowExceptionWhenTicketNotSold() {
        Ticket pendingTicket = givenPendingTicket();
        BaggageDtos.BaggageCreateRequest request = new BaggageDtos.BaggageCreateRequest(
                2L,
                25.0
        );

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(pendingTicket));

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot register baggage for ticket with status PENDING");

        verify(ticketRepository, times(1)).findById(2L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando peso es cero")
    void shouldThrowExceptionWhenWeightIsZero() {
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = new BaggageDtos.BaggageCreateRequest(
                1L,
                0.0
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Baggage weight must be greater than 0");

        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando peso es negativo")
    void shouldThrowExceptionWhenWeightIsNegative() {
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = new BaggageDtos.BaggageCreateRequest(
                1L,
                -5.0
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Baggage weight must be greater than 0");

        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando peso excede el máximo permitido")
    void shouldThrowExceptionWhenWeightExceedsMaximum() {
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = new BaggageDtos.BaggageCreateRequest(
                1L,
                35.0
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG"))
                .thenReturn(BigDecimal.valueOf(30.0));

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Baggage exceeds maximum weight per bag");

        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket ya tiene máximo de equipajes")
    void shouldThrowExceptionWhenTicketHasMaximumBaggage() {
        Ticket ticket = givenSoldTicket();
        BaggageDtos.BaggageCreateRequest request = givenCreateRequest();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG"))
                .thenReturn(BigDecimal.valueOf(30.0));
        when(baggageRepository.countByTicketId(1L)).thenReturn(2);
        when(configService.getValueAsInt("MAX_BAGGAGE_PER_TICKET")).thenReturn(2);

        assertThatThrownBy(() -> baggageService.registerBaggage(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket already has maximum allowed baggage");

        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe actualizar equipaje correctamente")
    void shouldUpdateBaggage() {
        Ticket ticket = givenSoldTicket();
        Baggage existingBaggage = givenBaggage(ticket);
        BaggageDtos.BaggageUpdateRequest request = givenUpdateRequest();

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(existingBaggage));
        when(configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG"))
                .thenReturn(BigDecimal.valueOf(35.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(existingBaggage);

        baggageService.updateBaggage(1L, request);

        verify(baggageRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).save(existingBaggage);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar equipaje inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentBaggage() {
        BaggageDtos.BaggageUpdateRequest request = givenUpdateRequest();

        when(baggageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baggageService.updateBaggage(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Baggage with ID 999 not found");

        verify(baggageRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar con peso que excede el máximo")
    void shouldThrowExceptionWhenUpdatingWithExcessiveWeight() {
        Ticket ticket = givenSoldTicket();
        Baggage existingBaggage = givenBaggage(ticket);
        BaggageDtos.BaggageUpdateRequest request = new BaggageDtos.BaggageUpdateRequest(
                40.0,
                null
        );

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(existingBaggage));
        when(configService.getValueAsBigDecimal("BAGGAGE_MAX_WEIGHT_KG"))
                .thenReturn(BigDecimal.valueOf(30.0));

        assertThatThrownBy(() -> baggageService.updateBaggage(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Baggage exceeds maximum weight per bag");

        verify(baggageRepository, never()).save(any(Baggage.class));
    }

    @Test
    @DisplayName("Debe actualizar solo los campos no nulos")
    void shouldUpdateOnlyNonNullFields() {
        Ticket ticket = givenSoldTicket();
        Baggage existingBaggage = givenBaggage(ticket);
        BaggageDtos.BaggageUpdateRequest partialRequest = new BaggageDtos.BaggageUpdateRequest(
                null,
                "NEW-TAG-ONLY"
        );

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(existingBaggage));
        when(baggageRepository.save(any(Baggage.class))).thenReturn(existingBaggage);

        baggageService.updateBaggage(1L, partialRequest);

        verify(baggageRepository, times(1)).save(existingBaggage);
        verify(configService, never()).getValueAsBigDecimal(anyString());
    }

    @Test
    @DisplayName("Debe obtener equipajes por ticket ID")
    void shouldGetBaggageByTicket() {
        Ticket ticket = givenSoldTicket();
        List<Baggage> baggages = List.of(
                givenBaggage(ticket),
                givenBaggage(ticket)
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(baggageRepository.findByTicketId(1L)).thenReturn(baggages);

        List<BaggageDtos.BaggageResponse> result = baggageService.getBaggageByTicket(1L);

        assertThat(result).hasSize(2);

        verify(ticketRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).findByTicketId(1L);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ticket no existe al buscar equipajes")
    void shouldThrowExceptionWhenTicketNotFoundInGetBaggage() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baggageService.getBaggageByTicket(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Ticket with ID 999 not found");

        verify(ticketRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).findByTicketId(anyLong());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando ticket no tiene equipajes")
    void shouldReturnEmptyListWhenTicketHasNoBaggage() {
        Ticket ticket = givenSoldTicket();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(baggageRepository.findByTicketId(1L)).thenReturn(List.of());

        List<BaggageDtos.BaggageResponse> result = baggageService.getBaggageByTicket(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe calcular tarifa cero cuando peso está dentro del límite gratuito")
    void shouldCalculateZeroFeeWhenWeightWithinFreeLimit() {
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));

        BigDecimal fee = baggageService.calculateBaggageFee(15.0);

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);

        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
    }

    @Test
    @DisplayName("Debe calcular tarifa correctamente cuando hay exceso de peso")
    void shouldCalculateFeeCorrectlyWhenWeightExceedsLimit() {
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));
        when(configService.getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG"))
                .thenReturn(BigDecimal.valueOf(3000));

        BigDecimal fee = baggageService.calculateBaggageFee(25.0);

        assertThat(fee).isEqualByComparingTo(BigDecimal.valueOf(15000));

        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG");
        verify(configService, times(1)).getValueAsBigDecimal("BAGGAGE_EXTRA_FEE_PER_KG");
    }

    @Test
    @DisplayName("Debe calcular tarifa cero cuando peso es exactamente el límite")
    void shouldCalculateZeroFeeWhenWeightEqualsLimit() {
        when(configService.getValueAsBigDecimal("BAGGAGE_FREE_LIMIT_KG"))
                .thenReturn(BigDecimal.valueOf(20.0));

        BigDecimal fee = baggageService.calculateBaggageFee(20.0);

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Debe eliminar equipaje correctamente")
    void shouldDeleteBaggage() {
        Ticket ticket = givenSoldTicket();
        Baggage baggage = givenBaggage(ticket);

        when(baggageRepository.findById(1L)).thenReturn(Optional.of(baggage));
        doNothing().when(baggageRepository).delete(baggage);

        baggageService.deleteBaggage(1L);

        verify(baggageRepository, times(1)).findById(1L);
        verify(baggageRepository, times(1)).delete(baggage);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar equipaje inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentBaggage() {
        when(baggageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baggageService.deleteBaggage(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Baggage with ID 999 not found");

        verify(baggageRepository, times(1)).findById(999L);
        verify(baggageRepository, never()).delete(any(Baggage.class));
    }
}