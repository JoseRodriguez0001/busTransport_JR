package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.IncidentDtos;
import com.unimag.bustransport.api.dto.ParcelDtos;
import com.unimag.bustransport.domain.entities.Parcel;
import com.unimag.bustransport.domain.entities.Stop;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.repositories.ParcelRepository;
import com.unimag.bustransport.domain.repositories.StopRepository;
import com.unimag.bustransport.domain.repositories.TripRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.IncidentService;
import com.unimag.bustransport.services.ParcelService;
import com.unimag.bustransport.services.mapper.ParcelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final IncidentService incidentService;
    private final ParcelMapper parcelMapper;
    private final Random random = new Random();

    @Override
    public ParcelDtos.ParcelResponse createParcel(ParcelDtos.ParcelCreateRequest request) {
        log.debug("Creating parcel");

        String code = generateUniqueParcelCode();

        log.debug("Generated parcel code: {}", code);

        // Validar que las paradas existan
        Stop fromStop = stopRepository.findById(request.fromStopId())
                .orElseThrow(() -> {
                    log.error("From stop not found with ID: {}", request.fromStopId());
                    return new NotFoundException(
                            String.format("Stop with ID %d not found", request.fromStopId())
                    );
                });

        Stop toStop = stopRepository.findById(request.toStopId())
                .orElseThrow(() -> {
                    log.error("To stop not found with ID: {}", request.toStopId());
                    return new NotFoundException(
                            String.format("Stop with ID %d not found", request.toStopId())
                    );
                });

        // Validar que las paradas pertenezcan a la misma ruta
        if (!fromStop.getRoute().getId().equals(toStop.getRoute().getId())) {
            log.error("Stops do not belong to the same route");
            throw new IllegalArgumentException(
                    "Origin and destination stops must belong to the same route"
            );
        }

        // Validar el orden de las paradas
        if (fromStop.getOrder() >= toStop.getOrder()) {
            log.error("Invalid stop order: from={}, to={}", fromStop.getOrder(), toStop.getOrder());
            throw new IllegalArgumentException(
                    "Origin stop order must be less than destination stop order"
            );
        }

        Parcel parcel = parcelMapper.toEntity(request);
        parcel.setCode(code);
        parcel.setFromStop(fromStop);
        parcel.setToStop(toStop);
        parcel.setStatus(Parcel.Status.CREATED);

        // Si se proporciona un trip, validarlo y asignarlo
        if (request.tripId() != null) {
            Trip trip = tripRepository.findById(request.tripId())
                    .orElseThrow(() -> {
                        log.error("Trip not found with ID: {}", request.tripId());
                        return new NotFoundException(
                                String.format("Trip with ID %d not found", request.tripId())
                        );
                    });

            // Validar que el trip pertenezca a la misma ruta
            if (!trip.getRoute().getId().equals(fromStop.getRoute().getId())) {
                log.error("Trip does not belong to the same route as the stops");
                throw new IllegalArgumentException(
                        "Trip must belong to the same route as the parcel stops"
                );
            }

            // Validar que el trip esté en estado apropiado
            if (trip.getStatus() == Trip.Status.ARRIVED || trip.getStatus() == Trip.Status.CANCELLED) {
                log.error("Cannot assign parcel to trip with status: {}", trip.getStatus());
                throw new IllegalStateException(
                        String.format("Cannot assign parcel to trip with status %s", trip.getStatus())
                );
            }

            parcel.setTrip(trip);
            parcel.setStatus(Parcel.Status.IN_TRANSIT);
            log.info("Parcel assigned to trip {} and marked as IN_TRANSIT", trip.getId());
        }

        // Generar OTP de entrega (6 dígitos)
        String otp = generateOtp();
        parcel.setDeliveryOtp(otp);

        Parcel savedParcel = parcelRepository.save(parcel);
        log.info("Parcel created with code: {} and OTP: {}", savedParcel.getCode(), otp);
        return parcelMapper.toResponse(savedParcel);
    }

    @Override
    public void updateParcel(Long parcelId, ParcelDtos.ParcelUpdateRequest request) {
        log.debug("Updating parcel with ID: {}", parcelId);

        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> {
                    log.error("Parcel not found with ID: {}", parcelId);
                    return new NotFoundException(
                            String.format("Parcel with ID %d not found", parcelId)
                    );
                });

        // Solo permitir editar si está en CREATED
        if (parcel.getStatus() != Parcel.Status.CREATED) {
            log.error("Cannot update parcel with status: {}", parcel.getStatus());
            throw new IllegalStateException(
                    String.format("Can only update parcels with status CREATED. Current status: %s. " +
                                    "Use specific methods for other operations (assignTrip, confirmDelivery, markAsFailed).",
                            parcel.getStatus())
            );
        }

        // Actualizar datos de personas
        if (request.senderName() != null) {
            parcel.setSenderName(request.senderName());
            log.debug("Updated sender name to: {}", request.senderName());
        }

        if (request.senderPhone() != null) {
            parcel.setSenderPhone(request.senderPhone());
            log.debug("Updated sender phone to: {}", request.senderPhone());
        }

        if (request.receiverName() != null) {
            parcel.setReceiverName(request.receiverName());
            log.debug("Updated receiver name to: {}", request.receiverName());
        }

        if (request.receiverPhone() != null) {
            parcel.setReceiverPhone(request.receiverPhone());
            log.debug("Updated receiver phone to: {}", request.receiverPhone());
        }

        // Actualizar precio
        if (request.price() != null) {
            parcel.setPrice(request.price());
            log.debug("Updated price to: {}", request.price());
        }

        // Actualizar paradas (con validaciones)
        if (request.fromStopId() != null || request.toStopId() != null) {
            Long newFromStopId = request.fromStopId() != null ? request.fromStopId() : parcel.getFromStop().getId();
            Long newToStopId = request.toStopId() != null ? request.toStopId() : parcel.getToStop().getId();

            Stop newFromStop = stopRepository.findById(newFromStopId)
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Stop with ID %d not found", newFromStopId)
                    ));

            Stop newToStop = stopRepository.findById(newToStopId)
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Stop with ID %d not found", newToStopId)
                    ));

            // Validar que pertenezcan a la misma ruta
            if (!newFromStop.getRoute().getId().equals(newToStop.getRoute().getId())) {
                throw new IllegalArgumentException(
                        "Origin and destination stops must belong to the same route"
                );
            }

            // Validar orden
            if (newFromStop.getOrder() >= newToStop.getOrder()) {
                throw new IllegalArgumentException(
                        "Origin stop order must be less than destination stop order"
                );
            }

            parcel.setFromStop(newFromStop);
            parcel.setToStop(newToStop);
            log.debug("Updated stops: from {} to {}", newFromStopId, newToStopId);
        }

        parcelRepository.save(parcel);
        log.info("Parcel {} updated successfully", parcelId);
    }

    @Override
    public void assignTrip(Long parcelId, Long tripId) {
        log.debug("Assigning trip {} to parcel {}", tripId, parcelId);

        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> {
                    log.error("Parcel not found with ID: {}", parcelId);
                    return new NotFoundException(
                            String.format("Parcel with ID %d not found", parcelId)
                    );
                });

        if (parcel.getStatus() != Parcel.Status.CREATED) {
            log.error("Cannot assign trip to parcel with status: {}", parcel.getStatus());
            throw new IllegalStateException(
                    String.format("Can only assign trip to parcels with status CREATED. Current status: %s",
                            parcel.getStatus())
            );
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> {
                    log.error("Trip not found with ID: {}", tripId);
                    return new NotFoundException(
                            String.format("Trip with ID %d not found", tripId)
                    );
                });

        if (!trip.getRoute().getId().equals(parcel.getFromStop().getRoute().getId())) {
            log.error("Trip route does not match parcel route");
            throw new IllegalArgumentException(
                    "Trip must belong to the same route as the parcel stops"
            );
        }

        if (trip.getStatus() == Trip.Status.ARRIVED || trip.getStatus() == Trip.Status.CANCELLED) {
            log.error("Cannot assign parcel to trip with status: {}", trip.getStatus());
            throw new IllegalStateException(
                    String.format("Cannot assign parcel to trip with status %s", trip.getStatus())
            );
        }

        parcel.setTrip(trip);
        parcel.setStatus(Parcel.Status.IN_TRANSIT);
        parcelRepository.save(parcel);

        log.info("Parcel {} assigned to trip {} and marked as IN_TRANSIT", parcelId, tripId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelDtos.ParcelResponse> getParcelsBySender(String senderPhone) {
        log.debug("Getting parcels by sender phone: {}", senderPhone);
        List<Parcel> parcels = parcelRepository.findBySenderPhone(senderPhone);

        log.info("Found {} parcels for sender phone: {}", parcels.size(), senderPhone);

        return parcels.stream()
                .map(parcelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelDtos.ParcelResponse> getParcelsByReceiver(String receiverPhone) {
        log.debug("Getting parcels by receiver phone: {}", receiverPhone);
        List<Parcel> parcels = parcelRepository.findByReceiverPhone(receiverPhone);

        log.info("Found {} parcels for receiver phone: {}", parcels.size(), receiverPhone);

        return parcels.stream()
                .map(parcelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParcelDtos.ParcelResponse> getParcelsByTrip(Long tripId) {
        log.debug("Getting parcels by trip ID: {}", tripId);

        if (!tripRepository.existsById(tripId)) {
            throw new NotFoundException(String.format("Trip with ID %d not found", tripId));
        }

        List<Parcel> parcels = parcelRepository.findByTripId(tripId);

        log.info("Found {} parcels for trip ID: {}", parcels.size(), tripId);

        return parcels.stream()
                .map(parcelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelDtos.ParcelResponse getParcelByCode(String code) {
        log.debug("Getting parcel by code: {}", code);
        Parcel parcel = parcelRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error("Parcel not found with code: {}", code);
                    return new NotFoundException(
                            String.format("Parcel with code %s not found", code)
                    );
                });
        return parcelMapper.toResponse(parcel);
    }

    @Override //123456
    public void confirmDelivery(Long parcelId, String otp, String proofPhotoUrl) {
        log.debug("Confirming delivery for parcel ID: {} with OTP: {}", parcelId, otp);
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> {
                    log.error("Parcel not found with ID: {}", parcelId);
                    return new NotFoundException(
                            String.format("Parcel with ID %d not found", parcelId)
                    );
                });

        if (parcel.getStatus() != Parcel.Status.IN_TRANSIT) {
            log.error("Parcel is not in transit. Current status: {}", parcel.getStatus());
            throw new IllegalStateException(
                    String.format("Parcel must be in transit to confirm delivery. Current status: %s",
                            parcel.getStatus())
            );
        }

        if (parcel.getDeliveryOtp() == null || !parcel.getDeliveryOtp().equals(otp)) {
            markAsFailed(parcelId, "different otp code or not exists");
            return;
        }

        if (proofPhotoUrl == null || proofPhotoUrl.isBlank()) {
            log.error("Proof photo URL is required for delivery confirmation");
            throw new IllegalArgumentException("Proof photo is required to confirm delivery");
        }

        parcel.setStatus(Parcel.Status.DELIVERED);
        parcel.setProofPhotoUrl(proofPhotoUrl);
        parcelRepository.save(parcel);

        log.info("Parcel delivery confirmed successfully for ID: {} with proof photo", parcelId);
    }

    @Override
    public void markAsFailed(Long parcelId, String failureReason) {
        log.debug("Marking parcel {} as failed with reason: {}", parcelId, failureReason);

        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> {
                    log.error("Parcel not found with ID: {}", parcelId);
                    return new NotFoundException(
                            String.format("Parcel with ID %d not found", parcelId)
                    );
                });

        if (parcel.getStatus() != Parcel.Status.IN_TRANSIT) {
            log.error("Can only mark IN_TRANSIT parcels as failed. Current status: {}", parcel.getStatus());
            throw new IllegalStateException(
                    String.format("Can only mark IN_TRANSIT parcels as failed. Current status: %s",
                            parcel.getStatus())
            );
        }

        parcel.setStatus(Parcel.Status.FAILED);
        parcelRepository.save(parcel);

        log.info("Parcel {} marked as FAILED. Reason: {}", parcelId, failureReason);

        try {
            IncidentDtos.IncidentCreateRequest incidentRequest = new IncidentDtos.IncidentCreateRequest(
                    "PARCEL",
                    parcelId,
                    "DELIVERY_FAIL",
                    failureReason != null ? failureReason : "Parcel delivery failed"
            );

            incidentService.createIncident(incidentRequest);
            log.info("Incident created automatically for failed parcel delivery: {}", parcelId);
        } catch (Exception e) {
            log.error("Failed to create incident for parcel {}: {}", parcelId, e.getMessage());
        }
    }

    private String generateUniqueParcelCode() {
        String code;
        do {
            code = generateParcelCode();
        } while (parcelRepository.existsByCode((code))); //para no repetir un codigo

        return code;
    }

    private String generateParcelCode() {
        // Formato: PAQ-YYYYMMDD-NNNN
        // Ejemplo: PAQ-20251109-0123

        LocalDate now = LocalDate.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Número secuencial aleatorio de 4 dígitos
        int sequenceNumber = random.nextInt(10000);
        String sequencePart = String.format("%04d", sequenceNumber);

        return String.format("PAQ-%s-%s", datePart, sequencePart);
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}