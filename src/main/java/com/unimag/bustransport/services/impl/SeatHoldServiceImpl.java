package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.SeatHoldDtos;
import com.unimag.bustransport.domain.entities.SeatHold;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.SeatHoldRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.SeatHoldService;
import com.unimag.bustransport.services.mapper.SeatHoldMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatHoldServiceImpl implements SeatHoldService {
    private final SeatHoldRepository seatHoldRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final SeatHoldMapper seatHoldMapper;
    private final ConfigService configService;

    @Override
    public SeatHoldDtos.SeatHoldResponse createSeatHold(SeatHoldDtos.SeatHoldCreateRequest request) {
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new NotFoundException("Viaje no encontrado"));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (isSeatOnHold(request.tripId(), request.seatNumber())) {
            throw new IllegalStateException(
                    "El asiento " + request.seatNumber() + " ya está reservado en este viaje"
            );
        }

        OffsetDateTime expiresAt = calculateExpirationTime();

        SeatHold seatHold = seatHoldMapper.toEntity(request);
        seatHold.setTrip(trip);
        seatHold.setUser(user);
        seatHold.setExpiresAt(expiresAt);
        seatHold.setStatus(SeatHold.Status.HOLD);

        seatHoldRepository.save(seatHold);
        return seatHoldMapper.toResponse(seatHold);
    }

    @Override
    public void releaseSeatHold(Long holdId) {
        SeatHold seatHold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("Reserva de asiento no encontrada"));

        seatHold.setStatus(SeatHold.Status.EXPIRED);
        seatHoldRepository.save(seatHold);
    }

    @Override
    public SeatHoldDtos.SeatHoldResponse getHoldById(Long holdId) {
        SeatHold seatHold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("Reserva de asiento no encontrada"));

        return seatHoldMapper.toResponse(seatHold);
    }

    @Override
    public List<SeatHoldDtos.SeatHoldResponse> getActiveHoldsByTripAndUser(Long tripId, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();

        List<SeatHold> activeHolds = seatHoldRepository
                .findByTripIdAndUserIdAndStatusAndExpiresAtAfter(
                        tripId,
                        userId,
                        SeatHold.Status.HOLD,
                        now
                );

        return activeHolds.stream().map(seatHoldMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatHoldDtos.SeatHoldResponse> getActiveHoldsByTrip(Long tripId) {
        OffsetDateTime now = OffsetDateTime.now();

        List<SeatHold> activeHolds = seatHoldRepository
                .findByTripIdAndStatusAndExpiresAtAfter(
                        tripId,
                        SeatHold.Status.HOLD,
                        now
                );

        return activeHolds.stream()
                .map(seatHoldMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatHoldDtos.SeatHoldResponse> getHoldsByUser(Long userId) {
        List<SeatHold> holds = seatHoldRepository.findByUserId(userId);

        return holds.stream()
                .map(seatHoldMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSeatOnHold(Long tripId, String seatNumber) {
        OffsetDateTime now = OffsetDateTime.now();
        return seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                tripId,
                seatNumber,
                SeatHold.Status.HOLD,
                now
        );
    }

    // Marca HOLD → EXPIRED (cuando pasa el tiempo y no alcanzo a crear compra)
    @Scheduled(cron = "0 */1 * * * *")  // Cada 1 minuto
    @Transactional
    @Override
    public int markExpiredHolds() {
        log.debug("Marking expired seat holds");

        List<SeatHold> expiredHolds = seatHoldRepository
                .findByStatusAndExpiresAtBefore(SeatHold.Status.HOLD, OffsetDateTime.now());

        if (expiredHolds.isEmpty()) {
            return 0;
        }

        expiredHolds.forEach(hold -> hold.setStatus(SeatHold.Status.EXPIRED));
        seatHoldRepository.saveAll(expiredHolds);

        log.info("Marked {} seat holds as EXPIRED", expiredHolds.size());
        return expiredHolds.size();
    }

    // Elimina todos los EXPIRED
    @Scheduled(cron = "0 */5 * * * *")  // Cada 5 minutos
    @Transactional
    @Override
    public int deleteExpiredHolds() {
        log.debug("Deleting EXPIRED seat holds");

        List<SeatHold> expiredHolds = seatHoldRepository.findByStatus((SeatHold.Status.EXPIRED));

        if (expiredHolds.isEmpty()) {
            return 0;
        }

        seatHoldRepository.deleteAll(expiredHolds);

        log.info("Deleted {} EXPIRED seat holds", expiredHolds.size());
        return expiredHolds.size();
    }

    @Override
    public void validateActiveHolds(Long tripId, List<String> seatNumbers, Long userId) {
        List<SeatHold> holds = seatHoldRepository.findByTripIdAndSeatNumberInAndStatus(
                tripId,
                seatNumbers,
                SeatHold.Status.HOLD
        );

        validateHoldsExist(holds, seatNumbers);
        validateHoldsOwnership(holds, userId);
        validateHoldsNotExpired(holds);
    }

    @Override
    public OffsetDateTime calculateExpirationTime() {
        Integer holdTimeMinutes = configService.getValueAsInt("HOLD_TIME_MIN");
        return OffsetDateTime.now().plusMinutes(holdTimeMinutes);
    }

    @Override
    public boolean hasOverlappingHold(Long tripId, String seatNumber, Integer fromStopOrder, Integer toStopOrder) {
        return seatHoldRepository.existsByTripIdAndSeatNumberAndStatusAndExpiresAtAfter(
                tripId,
                seatNumber,
                SeatHold.Status.HOLD,
                OffsetDateTime.now()
        );
    }

    private void validateHoldsExist(List<SeatHold> holds, List<String> seatNumbers) {
        if (holds.isEmpty()) {
            throw new IllegalStateException("No se encontraron reservas activas para los asientos especificados");
        }

        if (holds.size() != seatNumbers.size()) {
            throw new IllegalStateException("Algunos asientos no tienen reservas activas");
        }
    }

    private void validateHoldsOwnership(List<SeatHold> holds, Long userId) {
        for (SeatHold hold : holds) {
            if (!hold.getUser().getId().equals(userId)) {
                throw new IllegalStateException(
                        "El asiento " + hold.getSeatNumber() + " no pertenece al usuario"
                );
            }
        }
    }

    private void validateHoldsNotExpired(List<SeatHold> holds) {
        OffsetDateTime now = OffsetDateTime.now();

        for (SeatHold hold : holds) {
            if (hold.getExpiresAt().isBefore(now)) {
                throw new IllegalStateException(
                        "La reserva del asiento " + hold.getSeatNumber() + " ha expirado"
                );
            }
        }
    }
}
