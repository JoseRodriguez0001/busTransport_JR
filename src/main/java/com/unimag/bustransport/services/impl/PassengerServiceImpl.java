package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.PassengerDtos;
import com.unimag.bustransport.domain.entities.Passenger;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.PassengerRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.PassengerService;
import com.unimag.bustransport.services.mapper.PassengerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service@Transactional@RequiredArgsConstructor@Slf4j
public class PassengerServiceImpl implements PassengerService {
    private final PassengerRepository repository;
    private  final PassengerMapper mapper;
    private final UserRepository userRepository;

    @Override
    public PassengerDtos.PassengerResponse createPassenger(PassengerDtos.PassengerCreateRequest request) {
        Passenger passenger = mapper.toEntity(request);//creando entidad
        if (request.userId() != null) {
            log.debug("Asociando pasajero al usuario ID: {}", request.userId());
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> {return new NotFoundException(String.format("Usuario con ID %d no encontrado", request.userId())
                        );
                    });
            passenger.setUser(user);
        } else {
            log.debug("Creando pasajero sin usuario asociado (compra como invitado)");
        }

        passenger.setCreateAt(OffsetDateTime.now());//fecha de creacion
        Passenger savedPassenger = repository.save(passenger);//guardando
        log.info("Passenger creado");
        return mapper.toResponse(savedPassenger);//retorna informacion del passenger
    }

    @Override
    public void updatePassenger(Long id, PassengerDtos.PassengerUpdateRequest request) {
        Passenger passenger = repository.findById(id).orElseThrow(() -> {
            log.error("Pasajero no encontrado ");
            return new NotFoundException(
                    String.format("Pasajero con ID %d no encontrado", id)
            );
        });

        mapper.updateEntityFromRequest(request, passenger);
        repository.save(passenger);
        log.info("Pasajero actualizado");
    }

    @Override
    public void deletePassenger(Long id) {
        if (!repository.existsById(id)) {
            log.error("Pasajero no encontrado");
            throw new NotFoundException(String.format("Pasajero no encontrado"));
        }
        repository.deleteById(id);
        log.info("Pasajero eliminado");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerDtos.PassengerResponse> getPassengerByUser(Long userId) {
        List<Passenger> passengers = repository.findByUserId(userId);
        return passengers.stream()
                .map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerDtos.PassengerResponse finByDocumentNumber(String documentNumber) {
        Passenger passenger = repository.findByDocumentNumber(documentNumber).
                orElseThrow(() -> new NotFoundException(String.format("Pasajero no encontrado")));
        return mapper.toResponse(passenger);
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerDtos.PassengerResponse getPassengerById(Long id) {
        Passenger passenger = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Pasajero no encontrado")));
        return mapper.toResponse(passenger);
    }
}
